package com.restaurante.catalogo.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Solicitud para crear o actualizar un extra.
 */
public record ExtraRequest(
        @NotBlank(message = "El nombre del extra es obligatorio")
        @Size(max = 80, message = "El nombre no puede superar 80 caracteres")
        String nombre,

        @Size(max = 200, message = "La descripción no puede superar 200 caracteres")
        String descripcion,

        @NotNull(message = "El precio es obligatorio")
        @DecimalMin(value = "0.00", message = "El precio no puede ser negativo")
        BigDecimal precio) {
}