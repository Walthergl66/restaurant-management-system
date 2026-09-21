package com.restaurante.caja.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record AperturaRequest(
        @NotNull(message = "El monto inicial es obligatorio")
        @DecimalMin(value = "0.00", message = "El monto inicial no puede ser negativo")
        BigDecimal montoInicial) {
}