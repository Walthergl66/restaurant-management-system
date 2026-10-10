package com.restaurante.usuarios.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Reenvía el código de verificación al correo del cliente (RF-45). */
public record ReenviarVerificacionRequest(
        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no tiene un formato válido")
        String username) {
}
