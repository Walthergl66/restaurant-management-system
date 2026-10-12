package com.restaurante.catalogo.web.dto;

import jakarta.validation.constraints.NotNull;

/** Misma operación administrativa de habilitación que en mesas. */
public record HabilitacionCatalogoRequest(
        @NotNull(message = "La habilitación es obligatoria")
        Boolean habilitada) {
}
