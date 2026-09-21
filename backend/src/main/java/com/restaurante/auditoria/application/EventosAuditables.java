package com.restaurante.auditoria.application;

import com.restaurante.anulaciones.AnulacionAprobada;
import com.restaurante.pagos.CuentaCobrada;
import com.restaurante.pedidos.PedidoCancelado;
import com.restaurante.pedidos.PedidoConfirmado;
import com.restaurante.pedidos.PedidoCreado;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Escucha los eventos de negocio publicados por otros módulos (vía paquetes
 * raíz, sin violar Modulith) y los registra en el historial de auditoría.
 * La escritura va en una transacción aparte (REQUIRES_NEW en el servicio).
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

    @EventListener
    public void onPedidoCreado(PedidoCreado evento) {
        auditoria.registrar("PEDIDO_CREADO", "PEDIDO", evento.pedidoCodigo(),
                json(Map.of("mesaId", String.valueOf(evento.mesaId()))));
    }

    @EventListener
    public void onPedidoConfirmado(PedidoConfirmado evento) {
        auditoria.registrar("PEDIDO_CONFIRMADO", "PEDIDO", evento.pedidoCodigo(),
                json(Map.of("lineas", String.valueOf(evento.lineas().size()))));
    }

    @EventListener
    public void onPedidoCancelado(PedidoCancelado evento) {
        auditoria.registrar("PEDIDO_CANCELADO", "PEDIDO", evento.pedidoCodigo(),
                json(Map.of("mesaId", String.valueOf(evento.mesaId()))));
    }

    @EventListener
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

    @EventListener
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