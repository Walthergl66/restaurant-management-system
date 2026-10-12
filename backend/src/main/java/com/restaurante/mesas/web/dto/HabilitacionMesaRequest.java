package com.restaurante.mesas.web.dto;

import jakarta.validation.constraints.NotNull;

/** Solicitud administrativa para habilitar o deshabilitar una mesa. */
public record HabilitacionMesaRequest(
        @NotNull(message = "La habilitación es obligatoria")
        Boolean habilitada) {
}
