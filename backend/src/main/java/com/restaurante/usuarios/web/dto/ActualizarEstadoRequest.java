package com.restaurante.usuarios.web.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Cambio directo de estado (activo/inactivo) de un usuario desde la lista,
 * sin pasar por la actualización completa.
 */
public record ActualizarEstadoRequest(
        @NotNull(message = "El estado es obligatorio")
        Boolean activo) {
}
