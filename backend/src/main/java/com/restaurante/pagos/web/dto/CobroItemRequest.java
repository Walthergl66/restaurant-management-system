package com.restaurante.pagos.web.dto;

import com.restaurante.pagos.domain.MetodoPago;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Un pago del cobro (el cobro puede ser mixto, RF-31).
 */
public record CobroItemRequest(
        @NotNull(message = "El método de pago es obligatorio")
        MetodoPago metodo,

        @NotNull(message = "El monto es obligatorio")
        @DecimalMin(value = "0.01", message = "El monto debe ser mayor que cero")
        BigDecimal monto) {
}