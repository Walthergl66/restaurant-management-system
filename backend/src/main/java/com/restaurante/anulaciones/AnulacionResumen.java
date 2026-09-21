package com.restaurante.anulaciones;

import java.math.BigDecimal;

/**
 * Vista de una anulación para otros módulos (cuentas calculan el descuento
 * con las aprobadas, siempre desde los registros — RNF-16). El estado viaja
 * como String para no exponer el sub-paquete de dominio a otros módulos.
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
        String estado) {
}