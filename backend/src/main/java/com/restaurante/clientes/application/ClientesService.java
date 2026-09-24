package com.restaurante.clientes.application;

import com.restaurante.catalogo.Catalogo;
import com.restaurante.catalogo.ExtraParaPedido;
import com.restaurante.catalogo.ProductoParaPedido;
import com.restaurante.clientes.CarritoClienteSPI;
import com.restaurante.clientes.Clientes;
import com.restaurante.clientes.DireccionClienteSPI;
import com.restaurante.clientes.PedidoClienteConfirmado;
import com.restaurante.clientes.PedidoClienteEventos;
import com.restaurante.clientes.PedidoClienteSPI;
import com.restaurante.clientes.domain.Cliente;
import com.restaurante.clientes.domain.DireccionCliente;
import com.restaurante.clientes.domain.EstadoPedidoCliente;
import com.restaurante.clientes.domain.PedidoCliente;
import com.restaurante.clientes.domain.PedidoClienteLinea;
import com.restaurante.clientes.domain.PedidoClienteLineaExtra;
import com.restaurante.clientes.domain.PedidoClienteTablet;
import com.restaurante.clientes.infrastructure.ClienteRepository;
import com.restaurante.clientes.infrastructure.DireccionClienteRepository;
import com.restaurante.clientes.infrastructure.PedidoClienteRepository;
import com.restaurante.clientes.web.ConfirmarPedidoClienteRequest;
import com.restaurante.clientes.web.CrearPedidoClienteRequest;
import com.restaurante.clientes.web.NuevaDireccionClienteRequest;
import com.restaurante.shared.domain.Money;
import com.restaurante.shared.domain.exception.BusinessRuleException;
import com.restaurante.shared.domain.exception.ConflictException;
import com.restaurante.shared.domain.exception.NotFoundException;
import com.restaurante.shared.outbox.EventoOutbox;
import com.restaurante.shared.outbox.OutboxRepository;
import com.restaurante.usuarios.Usuarios;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@Transactional
public class ClientesService implements Clientes {

    private final PedidoClienteRepository pedidoRepository;
    private final ClienteRepository clienteRepository;
    private final DireccionClienteRepository direccionRepository;
    private final Usuarios usuarios;
    private final Catalogo catalogo;
    private final ApplicationEventPublisher eventPublisher;
    private final OutboxRepository outboxRepository;

    public ClientesService(PedidoClienteRepository pedidoRepository,
                           ClienteRepository clienteRepository,
                           DireccionClienteRepository direccionRepository,
                           Usuarios usuarios,
                           Catalogo catalogo,
                           ApplicationEventPublisher eventPublisher,
                           OutboxRepository outboxRepository) {
        this.pedidoRepository = pedidoRepository;
        this.clienteRepository = clienteRepository;
        this.direccionRepository = direccionRepository;
        this.usuarios = usuarios;
        this.catalogo = catalogo;
        this.eventPublisher = eventPublisher;
        this.outboxRepository = outboxRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public CarritoClienteSPI carrito(Long clienteId) {
        List<PedidoCliente> borradores = pedidoRepository.findByClienteIdAndEstado(clienteId, EstadoPedidoCliente.BORRADOR);
        List<PedidoClienteSPI> spis = borradores.stream().map(this::toSPI).toList();
        java.math.BigDecimal total = spis.stream()
                .map(PedidoClienteSPI::total)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        int totalItems = spis.stream().mapToInt(s -> s.lineas().size()).sum();
        return new CarritoClienteSPI(spis, Money.round(total), totalItems);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductoParaPedido> menu() {
        return catalogo.productosActivos();
    }

    @Override
    public PedidoClienteSPI crearPedido(Long clienteId, CrearPedidoClienteRequest request) {
        // Validar cliente existe
        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new NotFoundException("Cliente no encontrado: " + clienteId));

        // Validar codigo único global
        if (pedidoRepository.existsByCodigo(request.codigo())) {
            // Si es reintento idempotente con misma clave, devolver existente
            Optional<PedidoCliente> existente = pedidoRepository.findByCodigo(request.codigo());
            if (existente.isPresent() && existente.get().getClienteId().equals(clienteId)
                    && existente.get().getIdempotencyKey().equals(request.idempotencyKey())) {
                return toSPI(existente.get());
            }
            throw new ConflictException("Ya existe un pedido con el código " + request.codigo());
        }

        // Idempotencia por (clienteId, idempotencyKey): si ya existe con misma clave, es reintento
        Optional<PedidoCliente> porClave = pedidoRepository.findByClienteIdAndIdempotencyKey(clienteId, request.idempotencyKey());
        if (porClave.isPresent()) {
            // Si el código es distinto pero la clave igual, es conflicto de idempotencia (mismo cliente, misma clave, código distinto)
            // Devolvemos el existente si el código coincide, sino conflicto
            if (!porClave.get().getCodigo().equals(request.codigo())) {
                throw new ConflictException("Ya existe un pedido con la clave de idempotencia " + request.idempotencyKey());
            }
            return toSPI(porClave.get());
        }

        String metodoPago = validarMetodoPago(request.metodoPago());
        String metodoEntrega = validarMetodoEntrega(request.metodoEntrega());

        if ("DOMICILIO".equals(metodoEntrega)) {
            if (request.direccionId() == null) {
                throw new BusinessRuleException("La dirección es obligatoria para domicilio");
            }
            DireccionCliente dir = direccionRepository.findById(request.direccionId())
                    .orElseThrow(() -> new NotFoundException("Dirección no encontrada"));
            if (!dir.getClienteId().equals(clienteId)) {
                throw new BusinessRuleException("La dirección no pertenece al cliente");
            }
        }

        if (request.items() == null || request.items().isEmpty()) {
            throw new BusinessRuleException("El pedido debe tener al menos un producto");
        }

        PedidoCliente pedido = new PedidoCliente(
                request.codigo(),
                clienteId,
                metodoPago,
                metodoEntrega,
                request.direccionId(),
                request.idempotencyKey());

        for (CrearPedidoClienteRequest.ItemRequest item : request.items()) {
            ProductoParaPedido producto = catalogo.productoParaPedido(item.productoId())
                    .orElseThrow(() -> new NotFoundException("Producto no encontrado: " + item.productoId()));
            if (!producto.activo()) {
                throw new BusinessRuleException("El producto '" + producto.nombre() + "' no está activo");
            }
            PedidoClienteLinea linea = new PedidoClienteLinea(
                    producto.id(),
                    producto.nombre(),
                    producto.precio(),
                    item.cantidad(),
                    item.observaciones());
            if (item.extraIds() != null) {
                for (Long extraId : item.extraIds()) {
                    ExtraParaPedido extra = catalogo.extraParaPedido(extraId)
                            .orElseThrow(() -> new NotFoundException("Extra no encontrado: " + extraId));
                    if (!extra.activo()) {
                        throw new BusinessRuleException("El extra '" + extra.nombre() + "' no está activo");
                    }
                    PedidoClienteLineaExtra lineaExtra = new PedidoClienteLineaExtra(extra.id(), extra.nombre(), extra.precio());
                    linea.agregarExtra(lineaExtra);
                }
            }
            pedido.agregarLinea(linea);
        }

        pedidoRepository.save(pedido);
        // Recargar con lineas para SPI
        PedidoCliente guardado = pedidoRepository.findByCodigoWithLineas(pedido.getCodigo())
                .orElse(pedido);
        return toSPI(guardado);
    }

    @Override
    public PedidoClienteSPI confirmar(Long clienteId, String codigo, ConfirmarPedidoClienteRequest request) {
        String clave = request.idempotencyKey();
        PedidoCliente pedido = pedidoRepository.findByCodigoWithLineas(codigo)
                .orElseThrow(() -> new NotFoundException("Pedido no encontrado: " + codigo));
        protegerPedidoDe(clienteId, pedido);

        // Idempotencia: si no es BORRADOR, misma clave => 200, distinta => 409
        if (pedido.getEstado() != EstadoPedidoCliente.BORRADOR) {
            if (!pedido.getIdempotencyKey().equals(clave)) {
                throw new ConflictException("El pedido ya fue confirmado con otra clave (RF-41)");
            }
            return toSPI(pedido);
        }

        // Validar que la clave de confirmación coincide con la de creación (si se quiere estricto)
        if (!pedido.getIdempotencyKey().equals(clave)) {
            throw new ConflictException("La clave de confirmación no coincide con la del pedido (RF-41)");
        }

        pedido.confirmar(clave);
        pedidoRepository.save(pedido);
        publicarConfirmado(pedido);
        return toSPI(pedido);
    }

    @Override
    @Transactional(readOnly = true)
    public PedidoClienteTablet pedidoPorCodigo(String codigo) {
        PedidoCliente pedido = pedidoRepository.findByCodigoWithLineas(codigo)
                .orElseThrow(() -> new NotFoundException("Pedido no encontrado: " + codigo));
        PedidoClienteTablet tablet = new PedidoClienteTablet(pedido.getId(), pedido.getCodigo(), pedido.getClienteId(), pedido.getIdempotencyKey());
        // Reconstruir líneas para tablet
        for (PedidoClienteLinea l : pedido.getLineas()) {
            List<Money> extrasPrecios = l.getExtras().stream().map(PedidoClienteLineaExtra::getPrecio).toList();
            tablet.agregarLinea(new com.restaurante.clientes.domain.LineaPedidoClienteTabletLinea(
                    l.getProductoId(), l.getNombre(), l.getPrecio(), l.getCantidad(), extrasPrecios, l.getObservaciones()));
        }
        // Sincronizar estado
        // No hay método para setear estado directamente; usamos transiciones según estado actual
        // Si el pedido ya está CONFIRMADO o más, confirmamos el tablet
        if (pedido.getEstado() != EstadoPedidoCliente.BORRADOR) {
            try { tablet.confirmar(pedido.getIdempotencyKey()); } catch (Exception ignored) {}
            if (pedido.getEstado() == EstadoPedidoCliente.EN_PREPARACION || pedido.getEstado() == EstadoPedidoCliente.LISTO || pedido.getEstado() == EstadoPedidoCliente.ENTREGADO) {
                try { tablet.marcarEnPreparacion(); } catch (Exception ignored) {}
            }
            if (pedido.getEstado() == EstadoPedidoCliente.LISTO || pedido.getEstado() == EstadoPedidoCliente.ENTREGADO) {
                try { tablet.marcarListo(); } catch (Exception ignored) {}
            }
        }
        return tablet;
    }

    @Override
    public PedidoClienteTablet confirmar(String codigo, Object idempotencyKey) {
        String clave = idempotencyKey == null ? null : idempotencyKey.toString();
        PedidoCliente pedido = pedidoRepository.findByCodigoWithLineas(codigo)
                .orElseThrow(() -> new NotFoundException("Pedido no encontrado: " + codigo));
        if (pedido.getEstado() != EstadoPedidoCliente.BORRADOR) {
            if (clave == null || !pedido.getIdempotencyKey().equals(clave)) {
                throw new ConflictException("El pedido ya fue confirmado (RF-41)");
            }
            return pedidoPorCodigo(codigo);
        }
        if (!pedido.getIdempotencyKey().equals(clave)) {
            throw new ConflictException("Clave distinta (RF-41)");
        }
        pedido.confirmar(clave);
        pedidoRepository.save(pedido);
        publicarConfirmado(pedido);
        return pedidoPorCodigo(codigo);
    }

    @Override
    @Transactional(readOnly = true)
    public PedidoClienteSPI pedidoSPIporCodigo(Long clienteId, String codigo) {
        PedidoCliente pedido = pedidoRepository.findByCodigoWithLineas(codigo)
                .orElseThrow(() -> new NotFoundException("Pedido no encontrado: " + codigo));
        protegerPedidoDe(clienteId, pedido);
        return toSPI(pedido);
    }

    @Override
    public PedidoClienteSPI marcarEnPreparacion(String codigo) {
        PedidoCliente pedido = pedidoRepository.findByCodigoWithLineas(codigo)
                .orElseThrow(() -> new NotFoundException("Pedido no encontrado: " + codigo));
        pedido.marcarEnPreparacion();
        pedidoRepository.save(pedido);
        notificarEstado(pedido);
        return toSPI(pedido);
    }

    @Override
    public PedidoClienteSPI marcarListo(String codigo) {
        PedidoCliente pedido = pedidoRepository.findByCodigoWithLineas(codigo)
                .orElseThrow(() -> new NotFoundException("Pedido no encontrado: " + codigo));
        pedido.marcarListo();
        pedidoRepository.save(pedido);
        notificarEstado(pedido);
        return toSPI(pedido);
    }

    @Override
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<PedidoClienteSPI> historial(Long clienteId,
                                                                            org.springframework.data.domain.Pageable pageable) {
        return pedidoRepository.historialPaginado(clienteId, pageable).map(this::toSPI);
    }

    @Override
    public DireccionClienteSPI nuevaDireccion(Long clienteId, NuevaDireccionClienteRequest request) {
        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new NotFoundException("Cliente no encontrado: " + clienteId));
        DireccionCliente dir = new DireccionCliente(
                cliente.getId(),
                request.etiqueta(),
                request.direccion(),
                request.telefono(),
                request.observaciones());
        direccionRepository.save(dir);
        return toSPI(dir);
    }

    @Override
    public Long resolverClienteId(String username) {
        var usuario = usuarios.porUsername(username)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado: " + username));
        Long usuarioId = usuario.id();
        Optional<Cliente> opt = clienteRepository.findByUsuarioId(usuarioId);
        if (opt.isPresent()) {
            return opt.get().getId();
        }
        // Crear cliente on-the-fly (RF-42) con valores únicos determinísticos
        String base = java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String cedula = "C" + usuarioId + "-" + base.substring(0, 4);
        String telefono = "09" + base.substring(0, 8);
        // Asegurar unicidad en caso de colisión (hasta 5 intentos)
        int intentos = 0;
        while (clienteRepository.findByCedula(cedula).isPresent() && intentos < 5) {
            base = java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 8);
            cedula = "C" + usuarioId + "-" + base.substring(0, 4);
            intentos++;
        }
        intentos = 0;
        while (clienteRepository.findByTelefono(telefono).isPresent() && intentos < 5) {
            base = java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 8);
            telefono = "09" + base.substring(0, 8);
            intentos++;
        }
        Cliente nuevo = new Cliente(usuarioId, cedula, telefono, usuario.nombre());
        Cliente guardado = clienteRepository.saveAndFlush(nuevo);
        Long id = guardado.getId();
        if (id == null) {
            // Fallback: recargar por usuarioId
            return clienteRepository.findByUsuarioId(usuarioId)
                    .orElseThrow(() -> new IllegalStateException("No se pudo crear cliente para " + username))
                    .getId();
        }
        return id;
    }

    /** A-01: un pedido solo es operable por su propietario. 404 (no 403) para
     *  no revelar la existencia de códigos ajenos. */
    private void protegerPedidoDe(Long clienteId, PedidoCliente pedido) {
        if (!pedido.getClienteId().equals(clienteId)) {
            throw new NotFoundException("Pedido no encontrado: " + pedido.getCodigo());
        }
    }

    /**
     * Publica el evento en la MISMA transacción de la confirmación: las comandas
     * y su outbox se guardan junto con ella (RNF-17). El área se resuelve del
     * catálogo al confirmar, igual que en el flujo presencial.
     */
    private void publicarConfirmado(PedidoCliente pedido) {
        List<PedidoClienteConfirmado.LineaConfirmada> lineas = pedido.getLineas().stream()
                .map(l -> {
                    ProductoParaPedido producto =
                            catalogo.productoParaPedido(l.getProductoId()).orElse(null);
                    return new PedidoClienteConfirmado.LineaConfirmada(
                            l.getProductoId(),
                            l.getNombre(),
                            l.getCantidad(),
                            l.getExtras().stream().map(e -> e.getNombre()).toList(),
                            l.getObservaciones(),
                            producto == null ? null : producto.areaId(),
                            producto == null ? null : producto.areaNombre());
                })
                .toList();
        eventPublisher.publishEvent(new PedidoClienteConfirmado(pedido.getCodigo(), lineas));
        notificarEstado(pedido);
    }

    /** Escribe el aviso de cambio de estado en el outbox (tabla V4, misma
     *  transacción, RNF-17); el difusor lo consune y lo entrega por STOMP
     *  "al menos una vez" (RF-43). */
    private void notificarEstado(PedidoCliente pedido) {
        outboxRepository.save(new EventoOutbox(
                PedidoClienteEventos.TIPO_ESTADO,
                pedido.getCodigo(),
                pedido.getCodigo(),
                null, null, null,
                PedidoClienteEventos.EVENTO_ESTADO));
    }

    /** Vista congelada para difundir por STOMP (RF-43). En el mismo módulo,
     *  usado por {@code DifusorEstadoPedidoCliente}. */
    @Transactional(readOnly = true)
    public PedidoClienteSPI estadoParaDifusion(String codigo) {
        PedidoCliente pedido = pedidoRepository.findByCodigoWithLineas(codigo)
                .orElseThrow(() -> new NotFoundException("Pedido no encontrado: " + codigo));
        return toSPI(pedido);
    }

    private PedidoClienteSPI toSPI(PedidoCliente p) {
        // Necesitamos lineas con extras; si no están cargadas, las buscamos
        var lineas = p.getLineas();
        List<PedidoClienteSPI.LineaSPI> lineasSPI = lineas.stream().map(l -> {
            Money subtotal = l.subtotal();
            List<PedidoClienteSPI.LineaSPI.ExtraSPI> extrasSPI = l.getExtras().stream()
                    .map(e -> new PedidoClienteSPI.LineaSPI.ExtraSPI(e.getExtraId(), e.getNombre(), e.getPrecio().getAmount()))
                    .toList();
            return new PedidoClienteSPI.LineaSPI(
                    l.getProductoId(),
                    l.getNombre(),
                    l.getPrecio().getAmount(),
                    l.getCantidad(),
                    subtotal.getAmount(),
                    extrasSPI);
        }).toList();

        java.math.BigDecimal total = lineas.stream()
                .map(PedidoClienteLinea::subtotal)
                .reduce(Money.ZERO, Money::add)
                .getAmount();

        return new PedidoClienteSPI(
                p.getCodigo(),
                p.getEstado().name(),
                p.getMetodoPago(),
                p.getMetodoEntrega(),
                total,
                lineasSPI,
                p.getCreadoAt(),
                p.getActualizadoAt(),
                p.getVersion() == null ? 0 : p.getVersion());
    }

    private DireccionClienteSPI toSPI(DireccionCliente d) {
        return new DireccionClienteSPI(
                d.getId(),
                d.getClienteId(),
                d.getEtiqueta(),
                d.getDireccion(),
                d.getTelefono(),
                d.getObservaciones(),
                d.getActiva(),
                d.getCreadoAt());
    }

    private String validarMetodoPago(String mp) {
        if (mp == null) throw new BusinessRuleException("Método de pago obligatorio");
        String up = mp.trim().toUpperCase();
        if (!Set.of("EFECTIVO", "TARJETA", "TRANSFERENCIA").contains(up)) {
            throw new BusinessRuleException("Método de pago no soportado: " + mp);
        }
        return up;
    }

    private String validarMetodoEntrega(String me) {
        if (me == null) throw new BusinessRuleException("Método de entrega obligatorio");
        String up = me.trim().toUpperCase();
        if (!Set.of("RETIRAR", "DOMICILIO").contains(up)) {
            throw new BusinessRuleException("Método de entrega no soportado: " + me);
        }
        return up;
    }
}
