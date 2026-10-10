package com.restaurante.usuarios.web.dto;

import com.restaurante.shared.validation.PasswordSegura;
import jakarta.validation.constraints.NotBlank;

/**
 * Restablecimiento de contraseña con el token de un solo uso (RF-45).
 */
public record RestablecerPasswordRequest(
        @NotBlank(message = "El token es obligatorio") String token,

        @NotBlank(message = "La contraseña es obligatoria")
        @PasswordSegura
        String nuevaPassword) {
}
