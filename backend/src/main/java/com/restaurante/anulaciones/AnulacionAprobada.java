package com.restaurante.anulaciones;

import java.math.BigDecimal;

/**
 * Evento publicado al APROBAR una anulación, en la MISMA transacción. Las
 * comandas reaccionan generando la comanda de cancelación y su orden de
 * impresión (RF-20 a RF-23).
 */
public record AnulacionAprobada(
        Long anulacionId,
        String pedidoCodigo,
        Long lineaId,
        Long productoId,
        String nombreProducto,
        BigDecimal precioUnitario,
        int cantidad,
        String motivo,
        Long areaId,
        String areaNombre) {
}