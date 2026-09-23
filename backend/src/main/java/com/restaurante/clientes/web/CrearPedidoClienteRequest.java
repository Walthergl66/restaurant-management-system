package com.restaurante.clientes.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/** RF-41: crear pedido desde la app (carrito -> confirmar). */
public record CrearPedidoClienteRequest(
        @NotBlank(message = "El código es obligatorio")
        @Size(max = 40, message = "El código no puede superar 40 caracteres")
        String codigo,

        @NotBlank(message = "El método de pago es obligatorio")
        String metodoPago,

        @NotBlank(message = "El método de entrega es obligatorio")
        String metodoEntrega,

        Long direccionId,

        @NotBlank(message = "La clave de idempotencia es obligatoria")
        @Size(max = 100, message = "La clave no puede superar 100 caracteres")
        String idempotencyKey,

        @Size(max = 50, message = "Demasiadas líneas")
        List<ItemRequest> items) {

    public record ItemRequest(
            @NotBlank(message = "El producto es obligatorio")
            Long productoId,
            @NotBlank(message = "La cantidad es obligatoria")
            Integer cantidad,
            List<Long> extraIds,
            List<String> ingredientesRemovidos,
            @Size(max = 500, message = "Observaciones muy largas")
            String observaciones) {
    }
}
