package com.restaurante.catalogo.web.dto;

import com.restaurante.catalogo.domain.Categoria;

/**
 * Vista de una categoría para administración.
 */
public record CategoriaResponse(
        Long id,
        String nombre,
        String descripcion,
        int orden,
        boolean activo) {

    public static CategoriaResponse from(Categoria categoria) {
        return new CategoriaResponse(
                categoria.getId(),
                categoria.getNombre(),
                categoria.getDescripcion(),
                categoria.getOrden(),
                categoria.isActivo());
    }
}