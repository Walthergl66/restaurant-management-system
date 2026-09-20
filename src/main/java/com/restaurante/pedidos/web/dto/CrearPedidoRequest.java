package com.restaurante.pedidos.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Solicitud para crear un pedido en borrador ligado a una mesa presencial.
 * El {@code codigo} lo genera la app para poder reintentar sin duplicar.
 */
public record CrearPedidoRequest(
        @NotBlank(message = "El código del pedido es obligatorio")
        @Size(max = 40, message = "El código no puede superar 40 caracteres")
        String codigo,

        @NotNull(message = "La mesa es obligatoria")
        Long mesaId,

        @Size(max = 500, message = "Las notas no pueden superar 500 caracteres")
        String notas) {
}