package com.restaurante.usuarios.web.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Solicitud de recuperación de contraseña (RF-45). Por seguridad la respuesta
 * es siempre 204: no revela si el usuario existe.
 */
public record SolicitarRecuperacionRequest(
        @NotBlank(message = "El usuario es obligatorio") String username) {
}
