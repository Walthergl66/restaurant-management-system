package com.restaurante.usuarios.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Restablecimiento de contraseña con el token de un solo uso (RF-45).
 */
public record RestablecerPasswordRequest(
        @NotBlank(message = "El token es obligatorio") String token,

        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 6, max = 72, message = "La contraseña debe tener entre 6 y 72 caracteres")
        String nuevaPassword) {
}
