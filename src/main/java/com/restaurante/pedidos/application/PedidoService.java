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
import com.restaurante.pedidos.web.dto.ActualizarLineaRequest;
import com.restaurante.pedidos.web.dto.AgregarLineaRequest;
import com.restaurante.pedidos.web.dto.CrearPedidoRequest;
import com.restaurante.pedidos.web.dto.PedidoResponse;
import com.restaurante.shared.domain.exception.BusinessRuleException;
import com.restaurante.shared.domain.exception.ConflictException;
import com.restaurante.shared.domain.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Ciclo de vida del pedido: borrador, personalización, resumen y confirmación
 * (RF-05 a RF-10). Al confirmar se revalida el catálogo (RF-09), se congelan
 * los precios y la llave de idempotencia evita duplicar por reintentos.
 */
@Service
@Transactional
public class PedidoService {

    private static final int MAX_IDEMPOTENCY_KEY = 100;

    private final PedidoRepository pedidoRepository;
    private final Catalogo catalogo;
    private final Mesas mesas;

    public PedidoService(PedidoRepository pedidoRepository, Catalogo catalogo, Mesas mesas) {
        this.pedidoRepository = pedidoRepository;
        this.catalogo = catalogo;
        this.mesas = mesas;
    }

    public PedidoResponse crear(CrearPedidoRequest request) {
        if (pedidoRepository.existsByCodigo(request.codigo())) {
            throw new ConflictException("Ya existe un pedido con el código " + request.codigo());
        }
        mesas.ocuparMesa(request.mesaId());
        Pedido pedido = new Pedido(request.codigo(), request.mesaId(), request.notas());
        return PedidoResponse.from(pedidoRepository.save(pedido));
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

        if (pedido.getEstado() == EstadoPedido.CONFIRMADO) {
            if (pedido.getConfirmaciones().stream().anyMatch(c -> c.tieneLaMismaClave(clave))) {
                return PedidoResponse.from(pedido);
            }
            throw new ConflictException("El pedido ya fue confirmado");
        }

        revalidarCatalogo(pedido);
        pedido.confirmar();
        pedido.agregarConfirmacion(clave);
        pedidoRepository.save(pedido);
        return PedidoResponse.from(pedido);
    }

    /**
     * Cancela un borrador liberando la mesa. Solo aplica antes de confirmar.
     */
    public void cancelarBorrador(String codigo) {
        Pedido pedido = cargarConLineas(codigo);
        if (pedido.getEstado() != EstadoPedido.BORRADOR) {
            throw new BusinessRuleException("Solo un pedido en borrador puede cancelarse");
        }
        mesas.liberarMesa(pedido.getMesaId());
        pedidoRepository.delete(pedido);
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