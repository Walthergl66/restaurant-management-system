package com.restaurante.pagos.web.dto;

import java.math.BigDecimal;
import java.util.List;

public record CobroItemResponse(
        Long id,
        String metodo,
        BigDecimal monto) {
}