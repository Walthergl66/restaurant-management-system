package com.restaurante.anulaciones.web.dto;

import com.restaurante.anulaciones.domain.Anulacion;
import com.restaurante.anulaciones.domain.EstadoAnulacion;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Resumen de una anulación para la API.
 */
public record AnulacionResponse(
        Long id,
        String pedidoCodigo,
        Long lineaId,
        Long productoId,
        String nombreProducto,
        BigDecimal precioUnitario,
        int cantidad,
        String motivo,
        EstadoAnulacion estado,
        String solicitadoPor,
        String resueltoPor,
        Instant resueltaAt) {

    public static AnulacionResponse from(Anulacion anulacion) {
        return new AnulacionResponse(
                anulacion.getId(),
                anulacion.getPedidoCodigo(),
                anulacion.getLineaId(),
                anulacion.getProductoId(),
                anulacion.getNombreProducto(),
                anulacion.getPrecioUnitario().getAmount(),
                anulacion.getCantidad(),
                anulacion.getMotivo(),
                anulacion.getEstado(),
                anulacion.getSolicitadoPor(),
                anulacion.getResueltoPor(),
                anulacion.getResueltaAt());
    }
}