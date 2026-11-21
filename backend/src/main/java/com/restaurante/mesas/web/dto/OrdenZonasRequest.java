package com.restaurante.mesas.web.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/** Orden completo de las zonas: sus ids, en la posición deseada. */
public record OrdenZonasRequest(
        @NotEmpty(message = "Debes indicar el orden de las zonas")
        List<Long> ids) {
}
