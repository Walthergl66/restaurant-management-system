package com.restaurante.caja.web.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CierreRequest(
        @NotNull(message = "El monto real en caja es obligatorio")
        BigDecimal montoReal) {
}