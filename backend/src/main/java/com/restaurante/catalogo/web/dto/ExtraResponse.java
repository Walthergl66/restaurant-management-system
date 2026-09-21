package com.restaurante.catalogo.web.dto;

import com.restaurante.catalogo.domain.Extra;

import java.math.BigDecimal;

/**
 * Vista de un extra.
 */
public record ExtraResponse(
        Long id,
        String nombre,
        String descripcion,
        BigDecimal precio,
        boolean activo) {

    public static ExtraResponse from(Extra extra) {
        return new ExtraResponse(
                extra.getId(),
                extra.getNombre(),
                extra.getDescripcion(),
                extra.getPrecio().getAmount(),
                extra.isActivo());
    }
}