package com.restaurante.catalogo.web.dto;

import com.restaurante.catalogo.domain.Area;

/**
 * Vista de un área de preparación.
 */
public record AreaResponse(
        Long id,
        String nombre,
        String descripcion,
        boolean activo) {

    public static AreaResponse from(Area area) {
        return new AreaResponse(area.getId(), area.getNombre(), area.getDescripcion(), area.isActivo());
    }
}