package com.restaurante.mesas.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Solicitud para crear o renombrar una zona. */
public record ZonaRequest(
        @NotBlank(message = "El nombre de la zona es obligatorio")
        @Size(max = 60, message = "El nombre de la zona no puede superar 60 caracteres")
        String nombre) {
}
