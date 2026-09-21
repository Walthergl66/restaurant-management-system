package com.restaurante.caja.web.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record MovimientoResponse(
        Long id,
        Long cajaId,
        String tipo,
        String concepto,
        BigDecimal monto,
        String metodo,
        Long pagoId,
        Instant created_at) {
}