package com.restaurante.usuarios.web.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Cuerpo para renovar el token de acceso con un token de refresco.
 */
public record RefreshTokenRequest(
        @NotBlank(message = "El token de refresco es obligatorio") String refreshToken) {
}