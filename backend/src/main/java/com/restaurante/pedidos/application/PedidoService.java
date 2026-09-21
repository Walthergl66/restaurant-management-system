package com.restaurante.pedidos.application;

import com.restaurante.catalogo.Catalogo;
import com.restaurante.catalogo.ExtraParaPedido;
import com.restaurante.catalogo.ProductoParaPedido;
import com.restaurante.mesas.Mesas;
import com.restaurante.pedidos.domain.ExtraLinea;
import com.restaurante.pedidos.domain.EstadoPedido;
import com.restaurante.pedidos.domain.IngredienteRemovido;
import com.restaurante.pedidos.domain.Pedido;
import com.restaurante.pedidos.domain.PedidoLinea;
import com.restaurante.pedidos.infrastructure.PedidoRepository;
import com.restaurante.pedidos.PedidoCancelado;
import com.restaurante.pedidos.PedidoConfirmado;
import com.restaurante.pedidos.PedidoCreado;
import com.restaurante.pedidos.PedidoResumen;
import com.restaurante.pedidos.Pedidos;
import com.restaurante.pedidos.LineaResumen;
import com.restaurante.pedidos.Pedidos.VentaProducto;
import com.restaurante.pedidos.web.dto.ActualizarLineaRequest;
import com.restaurante.pedidos.web.dto.AgregarLineaRequest;
import com.restaurante.pedidos.web.dto.CrearPedidoRequest;
import com.restaurante.pedidos.web.dto.PedidoResponse;
import com.restaurante.shared.domain.Money;
import com.restaurante.shared.domain.exception.BusinessRuleException;
import com.restaurante.shared.domain.exception.ConflictException;
import com.restaurante.shared.domain.exception.NotFoundException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Ciclo de vida del pedido: borrador, personalización, resumen y confirmación
 * (RF-05 a RF-10). Al confirmar se revalida el catálogo (RF-09), se congelan
 * los precios y la llave de idempotencia evita duplicar por reintentos.
 */
@Service
@Transactional
public class PedidoService implements Pedidos {

    private static final int MAX_IDEMPOTENCY_KEY = 100;

    private final PedidoRepository pedidoRepository;
    private final Catalogo catalogo;
    private final Mesas mesas;
    private final ApplicationEventPublisher eventPublisher;

    public PedidoService(PedidoRepository pedidoRepository, Catalogo catalogo, Mesas mesas,
                         ApplicationEventPublisher eventPublisher) {
        this.pedidoRepository = pedidoRepository;
        this.catalogo = catalogo;
        this.mesas = mesas;
        this.eventPublisher = eventPublisher;
    }

    public PedidoResponse crear(CrearPedidoRequest request) {
        if (pedidoRepository.existsByCodigo(request.codigo())) {
            throw new ConflictException("Ya existe un pedido con el código " + request.codigo());
        }
        mesas.ocuparMesa(request.mesaId());
        Pedido pedido = new Pedido(request.codigo(), request.mesaId(), request.notas());
        pedidoRepository.save(pedido);
        eventPublisher.publishEvent(new PedidoCreado(request.codigo(), request.mesaId()));
        return PedidoResponse.from(pedido);
    }

    @Transactional(readOnly = true)
    public List<PedidoResponse> listar(Long mesaId) {
        List<Pedido> todos = pedidoRepository.findAll();
        return todos.stream()
                .filter(p -> p.getEstado() != EstadoPedido.ANULADO)
                .filter(p -> mesaId == null || p.getMesaId().equals(mesaId))
                .map(PedidoResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public PedidoResponse obtener(String codigo) {
        return PedidoResponse.from(cargarConLineas(codigo));
    }

    public PedidoResponse agregarLinea(String codigo, AgregarLineaRequest request) {
        Pedido pedido = cargarConLineas(codigo);
        ProductoParaPedido producto = productoDisponible(request.productoId());
        Set<ExtraLinea> extras = congelarExtras(request.extraIds());
        Set<IngredienteRemovido> removidos = request.ingredientesRemovidos() == null
                ? new LinkedHashSet<>()
                : request.ingredientesRemovidos().stream().map(IngredienteRemovido::new)
                        .collect(Collectors.toCollection(LinkedHashSet::new));
        pedido.agregarLinea(
                producto.id(),
                producto.nombre(),
                producto.precio(),
                request.cantidad(),
                extras,
                removidos,
                request.observaciones());
        pedidoRepository.save(pedido);
        return PedidoResponse.from(pedido);
    }

    public PedidoResponse actualizarLinea(String codigo, Long lineaId, ActualizarLineaRequest request) {
        Pedido pedido = cargarConLineas(codigo);
        pedido.actualizarLinea(
                lineaId,
                request.cantidad(),
                congelarExtras(request.extraIds()),
                request.ingredientesRemovidos() == null
                        ? new LinkedHashSet<>()
                        : request.ingredientesRemovidos().stream().map(IngredienteRemovido::new)
                                .collect(Collectors.toCollection(LinkedHashSet::new)),
                request.observaciones());
        pedidoRepository.save(pedido);
        return PedidoResponse.from(pedido);
    }

    public PedidoResponse quitarLinea(String codigo, Long lineaId) {
        Pedido pedido = cargarConLineas(codigo);
        pedido.quitarLinea(lineaId);
        pedidoRepository.save(pedido);
        return PedidoResponse.from(pedido);
    }

    /**
     * Confirma el pedido aplicando la clave de idempotencia (RNF-17).
     */
    public PedidoResponse confirmar(String codigo, String idempotencyKey) {
        String clave = validarClaveIdempotencia(idempotencyKey);
        Pedido pedido = cargarConLineas(codigo);

        // Reintento idempotente: si el pedido ya salió de BORRADOR (confirma­do,
        // en preparación, listo...), la misma llave debe devolver el pedido 200
        // y cualquier otra llave es un conflicto (RNF-17).
        if (pedido.getEstado() != EstadoPedido.BORRADOR) {
            if (pedido.getConfirmaciones().stream().anyMatch(c -> c.tieneLaMismaClave(clave))) {
                return PedidoResponse.from(pedido);
            }
            throw new ConflictException("El pedido ya fue confirmado");
        }

        revalidarCatalogo(pedido);
        pedido.confirmar();
        pedido.agregarConfirmacion(clave);
        pedidoRepository.save(pedido);
        publicarConfirmado(pedido);
        return PedidoResponse.from(pedido);
    }

    /**
     * Publica el evento en la MISMA transacción: las comandas y el outbox se
     * guardan junto con la confirmación (RNF-17, defensa 3).
     */
    private void publicarConfirmado(Pedido pedido) {
        List<PedidoConfirmado.LineaConfirmada> lineas = pedido.getLineas().stream()
                .map(l -> {
                    ProductoParaPedido producto =
                            catalogo.productoParaPedido(l.getProductoId()).orElse(null);
                    return new PedidoConfirmado.LineaConfirmada(
                            l.getProductoId(),
                            l.getNombreProducto(),
                            l.getCantidad(),
                            l.getExtras().stream().map(e -> e.getNombre()).toList(),
                            l.getIngredientesRemovidos().stream().map(i -> i.getNombre()).toList(),
                            l.getObservaciones().orElse(null),
                            producto == null ? null : producto.areaId(),
                            producto == null ? null : producto.areaNombre());
                })
                .toList();
        eventPublisher.publishEvent(new PedidoConfirmado(pedido.getCodigo(), lineas));
    }

    /**
     * Adición: un pedido nuevo ligado a la misma cuenta (RF-17 a RF-19).
     * La mesa ya está ocupada, así que {@code ocuparMesa} es idempotente.
     */
    @Override
    public String crearAdicion(Long mesaId, String codigo, String notas) {
        return crear(new CrearPedidoRequest(codigo, mesaId, notas)).codigo();
    }

    @Override
    public Optional<PedidoResumen> pedidoConfirmado(String pedidoCodigo) {
        return pedidoRepository.findByCodigoConRelaciones(pedidoCodigo)
                .filter(p -> p.getEstado() != EstadoPedido.BORRADOR && p.getEstado() != EstadoPedido.ANULADO)
                .map(this::aResumen);
    }

    @Override
    public List<PedidoResumen> pedidosDeMesa(Long mesaId) {
        return pedidoRepository.findByMesaIdAndEstadoNot(mesaId, EstadoPedido.ANULADO).stream()
                .map(this::aResumen)
                .toList();
    }

    @Override
    public List<PedidoResumen> pedidosConfirmadosDeMesa(Long mesaId) {
        return pedidoRepository.findByMesaIdAndEstadoNot(mesaId, EstadoPedido.ANULADO).stream()
                .filter(p -> p.getEstado() != EstadoPedido.BORRADOR)
                .map(this::aResumen)
                .toList();
    }

    @Override
    public boolean hayOtroPedidoEnMesa(Long mesaId, String pedidoCodigo) {
        return pedidoRepository.existeOtroPedidoEnMesa(mesaId, pedidoCodigo);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VentaProducto> ventasPorProducto(Instant desde, Instant hasta) {
        Map<String, VentaProducto> acumuladas = new LinkedHashMap<>();
        List<Pedido> pedidos = pedidoRepository.findByCreatedAtBetweenAndEstadoNotIn(
                desde, hasta, List.of(EstadoPedido.BORRADOR, EstadoPedido.ANULADO));
        for (Pedido pedido : pedidos) {
            for (PedidoLinea linea : pedido.getLineas()) {
                String clave = claveProducto(linea.getProductoId(), linea.getNombreProducto());
                VentaProducto actual = acumuladas.get(clave);
                Money monto = actual == null
                        ? linea.subtotal()
                        : linea.subtotal().add(Money.of(actual.monto()));
                acumuladas.put(clave, new VentaProducto(
                        String.valueOf(linea.getProductoId()),
                        linea.getNombreProducto(),
                        linea.getCantidad() + (actual == null ? 0 : actual.cantidad()),
                        monto.getAmount()));
            }
        }
        return acumuladas.values().stream().toList();
    }

    /**
     * Clave de agrupación con separador de control no imprimible: el nombre
     * congelado puede contener cualquier carácter visible, incluido "|".
     */
    private String claveProducto(Long productoId, String nombreProducto) {
        return productoId + "" + nombreProducto;
    }

    private PedidoResumen aResumen(Pedido pedido) {
        List<LineaResumen> lineas = pedido.getLineas().stream().map(l -> {
            ProductoParaPedido producto = catalogo.productoParaPedido(l.getProductoId()).orElse(null);
            return new LineaResumen(
                    l.getId(),
                    l.getProductoId(),
                    l.getNombreProducto(),
                    l.getCantidad(),
                    l.getPrecioUnitario().getAmount(),
                    l.subtotal().getAmount(),
                    producto == null ? null : producto.areaId(),
                    producto == null ? null : producto.areaNombre());
        }).toList();
        return new PedidoResumen(pedido.getCodigo(), pedido.getMesaId(), pedido.getEstado().name(), lineas);
    }

    @Override
    public void marcarEnPreparacion(String pedidoCodigo) {
        Pedido pedido = cargarConLineas(pedidoCodigo);
        pedido.marcarEnPreparacion();
    }

    @Override
    public void marcarListo(String pedidoCodigo) {
        Pedido pedido = cargarConLineas(pedidoCodigo);
        pedido.marcarListo();
    }

    /**
     * Cancela un borrador. Una adición no libera la mesa si la cuenta todavía
     * tiene otro pedido; solo cuando es el último pedido de la mesa se libera
     * (RF-17 a RF-19).
     */
    public void cancelarBorrador(String codigo) {
        Pedido pedido = cargarConLineas(codigo);
        if (pedido.getEstado() != EstadoPedido.BORRADOR) {
            throw new BusinessRuleException("Solo un pedido en borrador puede cancelarse");
        }
        if (!pedidoRepository.existeOtroPedidoEnMesa(pedido.getMesaId(), codigo)) {
            mesas.liberarMesa(pedido.getMesaId());
        }
        pedidoRepository.delete(pedido);
        eventPublisher.publishEvent(new PedidoCancelado(codigo, pedido.getMesaId()));
    }

    private void revalidarCatalogo(Pedido pedido) {
        for (PedidoLinea linea : pedido.getLineas()) {
            ProductoParaPedido producto = productoDisponible(linea.getProductoId());
            Set<ExtraLinea> extras = linea.getExtras().stream()
                    .map(e -> {
                        ExtraParaPedido extra = catalogo.extraParaPedido(e.getExtraId())
                                .filter(ExtraParaPedido::activo)
                                .orElseThrow(() -> new BusinessRuleException(
                                        "El extra '" + e.getNombre() + "' ya no está disponible"));
                        return new ExtraLinea(extra.id(), extra.nombre(), extra.precio());
                    })
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            linea.congelarCatalogo(producto.nombre(), producto.precio(), extras);
        }
    }

    private ProductoParaPedido productoDisponible(Long productoId) {
        return catalogo.productoParaPedido(productoId)
                .filter(ProductoParaPedido::activo)
                .orElseThrow(() -> new BusinessRuleException("El producto indicado no está disponible"));
    }

    private Set<ExtraLinea> congelarExtras(List<Long> extraIds) {
        if (extraIds == null || extraIds.isEmpty()) {
            return new LinkedHashSet<>();
        }
        Set<ExtraLinea> extras = new LinkedHashSet<>();
        for (Long extraId : extraIds) {
            ExtraParaPedido extra = catalogo.extraParaPedido(extraId)
                    .filter(ExtraParaPedido::activo)
                    .orElseThrow(() -> new BusinessRuleException("El extra indicado no está disponible"));
            extras.add(new ExtraLinea(extra.id(), extra.nombre(), extra.precio()));
        }
        return extras;
    }

    private String validarClaveIdempotencia(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new BusinessRuleException("Confirmar un pedido exige la cabecera Idempotency-Key");
        }
        if (idempotencyKey.length() > MAX_IDEMPOTENCY_KEY) {
            throw new BusinessRuleException("La Idempotency-Key no puede superar " + MAX_IDEMPOTENCY_KEY + " caracteres");
        }
        return idempotencyKey;
    }

    private Pedido cargarConLineas(String codigo) {
        return pedidoRepository.findByCodigoConRelaciones(codigo)
                .orElseThrow(() -> new NotFoundException("Pedido no encontrado: " + codigo));
    }
}