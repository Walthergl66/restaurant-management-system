package com.restaurante.clientes.web.dto;

import com.restaurante.clientes.domain.PedidoClienteLinea;
import com.restaurante.clientes.domain.PedidoClienteLineaExtra;
import com.restaurante.shared.domain.Money;

import java.math.BigDecimal;
import java.util.List;

/**
 * Vista de una línea de pedido del cliente con sus precios congelados.
 */
public record PedidoClienteLineaResponse(
        Long id,
        Long productoId,
        String nombre,
        BigDecimal precioUnitario,
        Integer cantidad,
        BigDecimal subtotal,
        String observaciones,
        List<PedidoClienteExtraResponse> extras,
        List<String> ingredientesRemovidos) {

    public static PedidoClienteLineaResponse from(PedidoClienteLinea linea) {
        List<PedidoClienteExtraResponse> extras = linea.getExtras().stream()
                .map(PedidoClienteExtraResponse::from)
                .toList();
        return new PedidoClienteLineaResponse(
                linea.getId(),
                linea.getProductoId(),
                linea.getNombre(),
                linea.getPrecio().getAmount(),
                linea.getCantidad(),
                linea.subtotal().getAmount(),
                linea.getObservaciones(),
                extras,
                List.of()); // ingredientes removidos no se modelan como entidad separada en PedidoClienteLinea
    }
}
