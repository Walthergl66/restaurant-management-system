package com.restaurante.clientes.web.dto;

import com.restaurante.clientes.domain.PedidoClienteLineaExtra;

import java.math.BigDecimal;

/**
 * Extra de una línea de pedido del cliente.
 */
public record PedidoClienteExtraResponse(
        Long id,
        String nombre,
        BigDecimal precio) {

    public static PedidoClienteExtraResponse from(PedidoClienteLineaExtra extra) {
        return new PedidoClienteExtraResponse(
                extra.getExtraId(),
                extra.getNombre(),
                extra.getPrecio().getAmount());
    }
}
