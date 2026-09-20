package com.restaurante.usuarios.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Nueva contraseña para un usuario.
 */
public record CambiarPasswordRequest(
        @NotBlank(message = "La nueva contraseña es obligatoria")
        @Size(min = 6, max = 72, message = "La contraseña debe tener entre 6 y 72 caracteres")
        String nuevaPassword) {
}