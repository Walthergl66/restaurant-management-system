package com.restaurante.clientes.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** RF-41: confirmar con idempotencia (misma clave 200, distinta 409). */
public record ConfirmarPedidoClienteRequest(
        @NotBlank(message = "La clave de idempotencia es obligatoria")
        @Size(max = 100)
        String idempotencyKey) {
}
