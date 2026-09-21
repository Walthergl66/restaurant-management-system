package com.restaurante.catalogo.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Solicitud para crear o actualizar un área de preparación.
 */
public record AreaRequest(
        @NotBlank(message = "El nombre del área es obligatorio")
        @Size(max = 80, message = "El nombre no puede superar 80 caracteres")
        String nombre,

        @Size(max = 200, message = "La descripción no puede superar 200 caracteres")
        String descripcion) {
}