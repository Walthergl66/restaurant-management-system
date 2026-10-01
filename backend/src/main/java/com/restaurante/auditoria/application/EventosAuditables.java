package com.restaurante.auditoria.application;

import com.restaurante.anulaciones.AnulacionAprobada;
import com.restaurante.pagos.CuentaCobrada;
import com.restaurante.pedidos.PedidoCancelado;
import com.restaurante.pedidos.PedidoConfirmado;
import com.restaurante.pedidos.PedidoCreado;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Escucha los eventos de negocio publicados por otros módulos (vía paquetes
 * raíz, sin violar Modulith) y los registra en el historial de auditoría.
 * <p>
 * La escucha es {@code AFTER_COMMIT}: el historial debe reflejar lo que
 * realmente ocurrió, no lo que se intentó. Con un listener síncrono normal el
 * registro entraba en su propia transacción ({@code REQUIRES_NEW}) y quedaba
 * aunque el negocio se revirtiera después, de modo que un cobro o un pedido
 * anulado aparecían como aplicados en el historial (RNF-10/RNF-11).
 */
@Component
public class EventosAuditables {

    private static final BigDecimal CIEN = new BigDecimal("100");

    private final AuditoriaService auditoria;
    private final ObjectMapper objectMapper;

    public EventosAuditables(AuditoriaService auditoria, ObjectMapper objectMapper) {
        this.auditoria = auditoria;
        this.objectMapper = objectMapper;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPedidoCreado(PedidoCreado evento) {
        auditoria.registrar("PEDIDO_CREADO", "PEDIDO", evento.pedidoCodigo(),
                json(Map.of("mesaId", String.valueOf(evento.mesaId()))));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPedidoConfirmado(PedidoConfirmado evento) {
        auditoria.registrar("PEDIDO_CONFIRMADO", "PEDIDO", evento.pedidoCodigo(),
                json(Map.of("lineas", String.valueOf(evento.lineas().size()))));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPedidoCancelado(PedidoCancelado evento) {
        auditoria.registrar("PEDIDO_CANCELADO", "PEDIDO", evento.pedidoCodigo(),
                json(Map.of("mesaId", String.valueOf(evento.mesaId()))));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAnulacionAprobada(AnulacionAprobada evento) {
        BigDecimal monto = evento.precioUnitario().multiply(BigDecimal.valueOf(evento.cantidad()));
        Map<String, String> detalle = new LinkedHashMap<>();
        detalle.put("producto", evento.nombreProducto());
        detalle.put("cantidad", String.valueOf(evento.cantidad()));
        detalle.put("monto", monto.toPlainString());
        detalle.put("motivo", evento.motivo());
        auditoria.registrar("ANULACION_APROBADA", "ANULACION", String.valueOf(evento.anulacionId()),
                json(detalle));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCuentaCobrada(CuentaCobrada evento) {
        StringBuilder pagos = new StringBuilder();
        for (CuentaCobrada.PagoResumen p : evento.pagos()) {
            if (pagos.length() > 0) {
                pagos.append(", ");
            }
            pagos.append(p.metodo()).append(' ').append(p.monto().toPlainString());
        }
        Map<String, String> detalle = new LinkedHashMap<>();
        detalle.put("total", evento.total().toPlainString());
        detalle.put("comprobante", evento.comprobanteCorrelativo());
        detalle.put("pagos", pagos.toString());
        auditoria.registrar("CUENTA_COBRADA", "CUENTA", String.valueOf(evento.cuentaId()),
                json(detalle));
    }

    private String json(Map<String, String> detalle) {
        try {
            return objectMapper.writeValueAsString(detalle);
        } catch (Exception e) {
            return "{ \"detalle\": \"no serializable\" }";
        }
    }
}