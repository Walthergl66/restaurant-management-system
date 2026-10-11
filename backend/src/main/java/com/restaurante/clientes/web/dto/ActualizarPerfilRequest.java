package com.restaurante.clientes.web.dto;

import com.restaurante.shared.validation.CelularValido;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * Actualización del perfil del cliente (RF-45). El nombre y la cédula no son
 * editables. El correo puede cambiarse pero requiere re-verificación.
 */
public record ActualizarPerfilRequest(
        @CelularValido
        String celular,

        @Email(message = "El correo no tiene un formato válido")
        @Size(max = 50, message = "El correo no puede superar 50 caracteres")
        String correo) {
}
