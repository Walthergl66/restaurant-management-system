package com.restaurante.usuarios.web.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Credenciales para iniciar sesión.
 */
public record LoginRequest(
        @NotBlank(message = "El usuario es obligatorio") String username,
        @NotBlank(message = "La contraseña es obligatoria") String password) {
}