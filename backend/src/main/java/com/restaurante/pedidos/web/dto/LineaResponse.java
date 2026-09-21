package com.restaurante.pedidos.web.dto;

import com.restaurante.pedidos.domain.PedidoLinea;

import java.math.BigDecimal;
import java.util.List;

/**
 * Vista de una línea de pedido con sus precios congelados.
 */
public record LineaResponse(
        Long id,
        Long productoId,
        String nombreProducto,
        BigDecimal precioUnitario,
        int cantidad,
        BigDecimal subtotal,
        String observaciones,
        List<ExtraLineaResponse> extras,
        List<String> ingredientesRemovidos) {

    public static LineaResponse from(PedidoLinea linea) {
        return new LineaResponse(
                linea.getId(),
                linea.getProductoId(),
                linea.getNombreProducto(),
                linea.getPrecioUnitario().getAmount(),
                linea.getCantidad(),
                linea.subtotal().getAmount(),
                linea.getObservaciones().orElse(null),
                linea.getExtras().stream().map(ExtraLineaResponse::from).toList(),
                linea.getIngredientesRemovidos().stream()
                        .map(i -> i.getNombre().strip().toLowerCase())
                        .toList());
    }
}