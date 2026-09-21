package com.restaurante.cuentas.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Solicitud de adición: un pedido nuevo ligado a la misma cuenta.
 */
public record AdicionRequest(
        @NotBlank(message = "El código del pedido es obligatorio")
        @Size(max = 40, message = "El código no puede superar 40 caracteres")
        String codigo,

        @Size(max = 500, message = "Las notas no pueden superar 500 caracteres")
        String notas) {
}