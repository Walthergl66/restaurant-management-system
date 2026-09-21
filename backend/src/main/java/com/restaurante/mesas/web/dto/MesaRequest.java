package com.restaurante.mesas.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Solicitud para crear o actualizar una mesa.
 */
public record MesaRequest(
        @NotNull(message = "El número de mesa es obligatorio")
        @Min(value = 1, message = "El número debe ser mayor que cero")
        Integer numero,

        @NotNull(message = "La capacidad es obligatoria")
        @Min(value = 1, message = "La capacidad debe ser al menos 1")
        @Max(value = 50, message = "La capacidad no puede superar 50")
        Integer capacidad,

        @Size(max = 100, message = "La ubicación no puede superar 100 caracteres")
        String ubicacion) {
}