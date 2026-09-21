package com.restaurante.pedidos;

import java.math.BigDecimal;

/**
 * Vista congelada de una línea de pedido para otros módulos (anulaciones,
 * cuentas...). Los precios ya vienen congelados al confirmar (RF-09).
 */
public record LineaResumen(
        Long lineaId,
        Long productoId,
        String nombreProducto,
        int cantidad,
        BigDecimal precioUnitario,
        BigDecimal subtotal,
        Long areaId,
        String areaNombre) {
}