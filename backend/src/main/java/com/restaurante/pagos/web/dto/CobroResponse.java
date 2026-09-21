package com.restaurante.pagos.web.dto;

import java.math.BigDecimal;
import java.util.List;

public record CobroResponse(
        Long cuentaId,
        Long cajaId,
        BigDecimal total,
        String comprobanteCorrelativo,
        List<CobroItemResponse> pagos) {
}