package com.restaurante.catalogo.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

/**
 * Solicitud para crear o actualizar un producto del catálogo.
 * null en {@code categoriaId} o {@code areaId} significa "sin categoría/área".
 */
public record ProductoRequest(
        @NotBlank(message = "El nombre del producto es obligatorio")
        @Size(max = 120, message = "El nombre no puede superar 120 caracteres")
        String nombre,

        @Size(max = 500, message = "La descripción no puede superar 500 caracteres")
        String descripcion,

        @Size(max = 300, message = "La URL de la imagen no puede superar 300 caracteres")
        String imagenUrl,

        @NotNull(message = "El precio es obligatorio")
        @DecimalMin(value = "0.00", message = "El precio no puede ser negativo")
        BigDecimal precio,

        Long categoriaId,

        Long areaId,

        @NotNull(message = "La lista de extras es obligatoria")
        Set<Long> extraIds,

        List<String> ingredientes) {
}