package com.restaurante.comandas.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ErrorImpresionRequest(
        @NotBlank
        @Size(max = 500)
        String motivo) {
}