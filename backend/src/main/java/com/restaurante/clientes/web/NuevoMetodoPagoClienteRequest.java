package com.restaurante.clientes.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * RF-45: alta de un método de pago guardado. Solo metadata no sensible; no
 * viaja ni se guarda el número completo de la tarjeta.
 */
public record NuevoMetodoPagoClienteRequest(
        @NotBlank(message = "El tipo es obligatorio")
        @Size(max = 20)
        String tipo,

        @NotBlank(message = "El alias es obligatorio")
        @Size(max = 40)
        String alias,

        @Size(max = 4, message = "Solo los últimos 4 dígitos")
        String ultimos4,

        Boolean predeterminado) {
}
