package com.restaurante.catalogo.web.dto;

import java.util.List;

/**
 * Categoría del menú público con sus productos activos.
 */
public record CategoriaMenuDto(
        Long id,
        String nombre,
        List<ProductoMenuDto> productos) {

    public static CategoriaMenuDto of(Long id, String nombre, List<ProductoMenuDto> productos) {
        return new CategoriaMenuDto(id, nombre, productos);
    }
}