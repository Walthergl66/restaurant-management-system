package com.restaurante.caja.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record EgresoRequest(
        @NotBlank(message = "El concepto es obligatorio")
        @Size(max = 120, message = "El concepto no puede superar 120 caracteres")
        String concepto,

        @NotNull(message = "El monto es obligatorio")
        @DecimalMin(value = "0.01", message = "El monto debe ser mayor que cero")
        BigDecimal monto,

        @Size(max = 20, message = "El método no puede superar 20 caracteres")
        String metodo) {
}