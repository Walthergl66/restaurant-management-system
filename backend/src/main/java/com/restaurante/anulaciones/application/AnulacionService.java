package com.restaurante.anulaciones.application;

import com.restaurante.anulaciones.Anulaciones;
import com.restaurante.anulaciones.Anulaciones.DescuentoProducto;
import com.restaurante.anulaciones.AnulacionAprobada;
import com.restaurante.anulaciones.AnulacionResumen;
import com.restaurante.anulaciones.domain.Anulacion;
import com.restaurante.anulaciones.domain.EstadoAnulacion;
import com.restaurante.anulaciones.infrastructure.AnulacionRepository;
import com.restaurante.anulaciones.web.dto.AnulacionResponse;
import com.restaurante.anulaciones.web.dto.SolicitarAnulacionRequest;
import com.restaurante.pedidos.LineaResumen;
import com.restaurante.pedidos.PedidoResumen;
import com.restaurante.pedidos.Pedidos;
import com.restaurante.shared.domain.Money;
import com.restaurante.shared.domain.exception.BusinessRuleException;
import com.restaurante.shared.domain.exception.NotFoundException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Anulaciones de líneas (RF-20 a RF-23). La anulación es un registro aparte:
 * al aprobarse se descuenta de la cuenta y se publica el evento que genera la
 * comanda de cancelación; la línea original nunca se borra ni se edita.
 */
@Service
@Transactional
public class AnulacionService implements Anulaciones {

    private final AnulacionRepository anulacionRepository;
    private final Pedidos pedidos;
    private final ApplicationEventPublisher eventPublisher;

    public AnulacionService(AnulacionRepository anulacionRepository, Pedidos pedidos,
                            ApplicationEventPublisher eventPublisher) {
        this.anulacionRepository = anulacionRepository;
        this.pedidos = pedidos;
        this.eventPublisher = eventPublisher;
    }

    public AnulacionResponse solicitar(String pedidoCodigo, Long lineaId, SolicitarAnulacionRequest request) {
        PedidoResumen pedido = pedidos.pedidoConfirmado(pedidoCodigo)
                .orElseThrow(() -> new NotFoundException("Pedido no encontrado: " + pedidoCodigo));
        LineaResumen linea = pedido.lineas().stream()
                .filter(l -> l.lineaId().equals(lineaId))
                .findFirst()
                .orElseThrow(() -> new BusinessRuleException(
                        "La línea indicada no pertenece al pedido " + pedidoCodigo));

        // Lo que no se puede volver a comprometer ni aprobar: las unidades ya
        // aprobadas más las que hay en solicitudes SOLICITADA que aún no se
        // resolvieron (rechazada o aprobada). Reservar también lo pendiente
        // impide que dos solicitudes se peleen la misma unidad y, al aprobar
        // la segunda, la cuenta quede con total negativo (invariante RNF-16).
        int yaAprobada = anulacionRepository.cantidadAprobada(pedidoCodigo, lineaId);
        int yaSolicitada = anulacionRepository.cantidadSolicitada(pedidoCodigo, lineaId);
        int comprometido = yaAprobada + yaSolicitada;
        int cantidad = request.cantidad();
        if (comprometido + cantidad > linea.cantidad()) {
            throw new BusinessRuleException(
                    "No se puede anular más de la cantidad pedida (restante: "
                            + (linea.cantidad() - comprometido) + ")");
        }

        Anulacion anulacion = new Anulacion(
                pedidoCodigo,
                linea.lineaId(),
                linea.productoId(),
                linea.nombreProducto(),
                Money.of(linea.precioUnitario()),
                cantidad,
                request.motivo(),
                linea.areaId(),
                linea.areaNombre(),
                usuarioActual());
        return AnulacionResponse.from(anulacionRepository.save(anulacion));
    }

    public AnulacionResponse aprobar(Long id) {
        Anulacion anulacion = cargar(id);

        // Última barrera antes de mover dinero: al aprobar se comprueba el saldo
        // real de la línea. La validación de la solicitud no basta por sí sola
        // (dos cajeros pueden resolver a la vez y dejarían solicitudes que se
        // cruzaron; también hay solicitudes heredadas de antes del saldo con
        // pendientes). Si el saldo ya no alcanza, la cuenta quedaría con total
        // negativo, y CalculadoraCuenta lo rechazaría con un error confuso
        // en vez de un 422 claro.
        //
        // El pedido se lee con lock pesimista: si otra transacción está
        // resolviendo una anulación de la misma línea, esta espera y relee el
        // saldo ya actualizado en vez de decidir sobre un valor obsoleto.
        PedidoResumen pedido = pedidos.pedidoConfirmadoParaActualizar(anulacion.getPedidoCodigo())
                .orElseThrow(() -> new NotFoundException(
                        "Pedido no encontrado: " + anulacion.getPedidoCodigo()));
        int cantidadLinea = pedido.lineas().stream()
                .filter(l -> l.lineaId().equals(anulacion.getLineaId()))
                .map(LineaResumen::cantidad)
                .findFirst()
                .orElse(0);
        int yaAprobada = anulacionRepository.cantidadAprobada(
                anulacion.getPedidoCodigo(), anulacion.getLineaId());
        int otrasPendientes = anulacionRepository.cantidadSolicitada(
                anulacion.getPedidoCodigo(), anulacion.getLineaId()) - anulacion.getCantidad();
        if (yaAprobada + otrasPendientes + anulacion.getCantidad() > cantidadLinea) {
            throw new BusinessRuleException(
                    "No queda saldo anulable suficiente en la línea "
                            + anulacion.getLineaId() + " (pedido: " + cantidadLinea
                            + ", ya aprobado: " + yaAprobada
                            + ", pendiente por resolver: " + Math.max(0, otrasPendientes) + ")");
        }

        anulacion.aprobar(usuarioActual());
        eventPublisher.publishEvent(new AnulacionAprobada(
                anulacion.getId(),
                anulacion.getPedidoCodigo(),
                anulacion.getLineaId(),
                anulacion.getProductoId(),
                anulacion.getNombreProducto(),
                anulacion.getPrecioUnitario().getAmount(),
                anulacion.getCantidad(),
                anulacion.getMotivo(),
                anulacion.getAreaId(),
                anulacion.getAreaNombre()));
        return AnulacionResponse.from(anulacion);
    }

    public AnulacionResponse rechazar(Long id) {
        Anulacion anulacion = cargar(id);
        anulacion.rechazar(usuarioActual());
        return AnulacionResponse.from(anulacion);
    }

    @Transactional(readOnly = true)
    public List<AnulacionResponse> listar(String pedidoCodigo, EstadoAnulacion estado) {
        List<Anulacion> anulaciones;
        if (pedidoCodigo != null) {
            if (estado != null) {
                anulaciones = anulacionRepository.findByPedidoCodigoAndEstado(pedidoCodigo, estado);
            } else {
                anulaciones = anulacionRepository.findByPedidoCodigo(pedidoCodigo);
            }
        } else if (estado != null) {
            anulaciones = anulacionRepository.findByEstado(estado);
        } else {
            anulaciones = anulacionRepository.findTop100ByOrderByIdDesc();
        }
        return anulaciones.stream().map(AnulacionResponse::from).toList();
    }

    @Override
    public List<AnulacionResumen> aprobadasDe(List<String> pedidoCodigos) {
        return anulacionRepository.findByPedidoCodigoIn(pedidoCodigos).stream()
                .filter(a -> a.getEstado() == EstadoAnulacion.APROBADA)
                .map(a -> new AnulacionResumen(
                        a.getId(),
                        a.getPedidoCodigo(),
                        a.getLineaId(),
                        a.getProductoId(),
                        a.getNombreProducto(),
                        a.getPrecioUnitario().getAmount(),
                        a.getCantidad(),
                        a.getMotivo(),
                        a.getAreaId(),
                        a.getAreaNombre(),
                        a.getEstado().name()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DescuentoProducto> aprobadasPorProducto(Instant desde, Instant hasta) {
        Map<String, DescuentoProducto> acumuladas = new LinkedHashMap<>();
        List<Anulacion> aprobadas = anulacionRepository
                .findByCreatedAtBetweenAndEstado(desde, hasta, EstadoAnulacion.APROBADA);
        for (Anulacion anulacion : aprobadas) {
            String clave = claveProducto(anulacion.getProductoId(), anulacion.getNombreProducto());
            DescuentoProducto actual = acumuladas.get(clave);
            Money monto = anulacion.getPrecioUnitario()
                    .multiply(BigDecimal.valueOf(anulacion.getCantidad()));
            if (actual != null) {
                monto = monto.add(Money.of(actual.monto()));
            }
            acumuladas.put(clave, new DescuentoProducto(
                    String.valueOf(anulacion.getProductoId()),
                    anulacion.getNombreProducto(),
                    anulacion.getCantidad() + (actual == null ? 0 : actual.cantidad()),
                    monto.getAmount()));
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

    private Anulacion cargar(Long id) {
        return anulacionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Anulación " + id + " no encontrada"));
    }

    

    private String usuarioActual() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return "sistema";
        }
        return authentication.getName();
    }
}