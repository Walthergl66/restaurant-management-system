package com.restaurante.catalogo.web.dto;

import com.restaurante.catalogo.domain.Producto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Producto visible en el menú público.
 */
public record ProductoMenuDto(
        Long id,
        String nombre,
        String descripcion,
        String imagenUrl,
        BigDecimal precio,
        List<ExtraResponse> extras,
        List<String> ingredientes) {

    public static ProductoMenuDto from(Producto producto) {
        return new ProductoMenuDto(
                producto.getId(),
                producto.getNombre(),
                producto.getDescripcion(),
                producto.getImagenUrl(),
                producto.getPrecio().getAmount(),
                producto.getExtras().stream().map(ExtraResponse::from).toList(),
                producto.getIngredientes().stream()
                        .filter(i -> i.isActivo())
                        .map(i -> i.getNombre())
                        .toList());
    }
}