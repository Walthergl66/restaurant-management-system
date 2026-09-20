package com.restaurante.pedidos.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Solicitud para agregar una línea a un pedido en borrador.
 * Los precios NO vienen del cliente: se congelan desde el catálogo.
 */
public record AgregarLineaRequest(
        @NotNull(message = "El producto es obligatorio")
        Long productoId,

        @NotNull(message = "La cantidad es obligatoria")
        @Min(value = 1, message = "La cantidad debe ser mayor a cero")
        Integer cantidad,

        @Size(max = 50, message = "Demasiados extras")
        List<Long> extraIds,

        @Size(max = 20, message = "Demasiados ingredientes removidos")
        List<String> ingredientesRemovidos,

        @Size(max = 500, message = "Las observaciones no pueden superar 500 caracteres")
        String observaciones) {
}