package com.restaurante.clientes.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

/** RF-41: crear pedido desde la app (carrito -> confirmar). A-06: validación
 *  en cascada de cada línea y restricciones de tipo/límites para que las
 *  entradas inválidas fallen con 400 controlado (no NPE ni 500). */
public record CrearPedidoClienteRequest(
        @NotBlank(message = "El código es obligatorio")
        @Size(max = 40, message = "El código no puede superar 40 caracteres")
        String codigo,

        @NotBlank(message = "El método de pago es obligatorio")
        @Size(max = 20, message = "El método de pago no puede superar 20 caracteres")
        String metodoPago,

        @NotBlank(message = "El método de entrega es obligatorio")
        @Size(max = 20, message = "El método de entrega no puede superar 20 caracteres")
        String metodoEntrega,

        Long direccionId,

        @NotBlank(message = "La clave de idempotencia es obligatoria")
        @Size(max = 100, message = "La clave no puede superar 100 caracteres")
        String idempotencyKey,

        @NotNull(message = "Las líneas del pedido son obligatorias")
        @Size(min = 1, max = 50, message = "El pedido debe tener entre 1 y 50 líneas")
        List<@NotNull @Valid ItemRequest> items) {

    public record ItemRequest(
            @NotNull(message = "El producto es obligatorio")
            Long productoId,

            @NotNull(message = "La cantidad es obligatoria")
            @Positive(message = "La cantidad debe ser mayor que cero")
            Integer cantidad,

            @Size(max = 20, message = "Demasiados extras por línea")
            List<@NotNull Long> extraIds,

            @Size(max = 20, message = "Demasiados ingredientes removidos por línea")
            List<@Size(max = 50, message = "Ingrediente demasiado largo") String> ingredientesRemovidos,

            @Size(max = 500, message = "Observaciones muy largas")
            String observaciones) {
    }
}
