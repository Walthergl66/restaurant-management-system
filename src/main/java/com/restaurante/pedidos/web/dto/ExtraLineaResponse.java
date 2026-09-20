package com.restaurante.pedidos.web.dto;

import com.restaurante.pedidos.domain.ExtraLinea;

import java.math.BigDecimal;

/**
 * Vista de un extra congelado en una línea de pedido.
 */
public record ExtraLineaResponse(
        Long extraId,
        String nombre,
        BigDecimal precio) {

    public static ExtraLineaResponse from(ExtraLinea extra) {
        return new ExtraLineaResponse(
                extra.getExtraId(),
                extra.getNombre(),
                extra.getPrecio().getAmount());
    }
}