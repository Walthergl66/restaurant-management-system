package com.restaurante.configuracion.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Solicitud para actualizar un parámetro de configuración existente.
 */
public record ParametroRequest(
        @NotBlank(message = "El valor es obligatorio")
        @Size(max = 500, message = "El valor no puede superar 500 caracteres")
        String valor) {
}