package com.restaurante.anulaciones;

import com.restaurante.anulaciones.domain.EstadoAnulacion;

import java.math.BigDecimal;

/**
 * Vista de una anulación para otros módulos (cuentas calculan el descuento
 * con las aprobadas, siempre desde los registros — RNF-16).
 */
public record AnulacionResumen(
        Long id,
        String pedidoCodigo,
        Long lineaId,
        Long productoId,
        String nombreProducto,
        BigDecimal precioUnitario,
        int cantidad,
        String motivo,
        Long areaId,
        String areaNombre,
        EstadoAnulacion estado) {
}