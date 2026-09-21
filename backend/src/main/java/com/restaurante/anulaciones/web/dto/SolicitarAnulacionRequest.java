package com.restaurante.anulaciones.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * Solicitud de anulación de una línea ya confirmada.
 */
public record SolicitarAnulacionRequest(
        @Min(value = 1, message = "La cantidad debe ser al menos 1")
        @Max(value = 999, message = "La cantidad no puede superar 999")
        int cantidad,

        @Size(max = 300, message = "El motivo no puede superar 300 caracteres")
        String motivo) {
}