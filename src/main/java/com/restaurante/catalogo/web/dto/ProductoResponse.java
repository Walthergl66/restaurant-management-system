package com.restaurante.catalogo.web.dto;

import com.restaurante.catalogo.domain.IngredienteRemovible;
import com.restaurante.catalogo.domain.Producto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Vista de un producto para administración.
 */
public record ProductoResponse(
        Long id,
        String nombre,
        String descripcion,
        String imagenUrl,
        BigDecimal precio,
        Long categoriaId,
        String categoriaNombre,
        Long areaId,
        String areaNombre,
        boolean activo,
        List<ExtraResponse> extras,
        List<String> ingredientes) {

    public static ProductoResponse from(Producto producto) {
        return new ProductoResponse(
                producto.getId(),
                producto.getNombre(),
                producto.getDescripcion(),
                producto.getImagenUrl(),
                producto.getPrecio().getAmount(),
                producto.getCategoria() == null ? null : producto.getCategoria().getId(),
                producto.getCategoria() == null ? null : producto.getCategoria().getNombre(),
                producto.getArea() == null ? null : producto.getArea().getId(),
                producto.getArea() == null ? null : producto.getArea().getNombre(),
                producto.isActivo(),
                producto.getExtras().stream().map(ExtraResponse::from).toList(),
                producto.getIngredientes().stream()
                        .filter(IngredienteRemovible::isActivo)
                        .map(IngredienteRemovible::getNombre)
                        .toList());
    }
}