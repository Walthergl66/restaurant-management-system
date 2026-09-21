package com.restaurante.pagos.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * Solicitud de cobro: la lista de pagos (mixta) y, opcionalmente, los datos
 * del comprobante (si no trae datos se emite TICKET).
 */
public record CobroRequest(
        @Valid
        @NotEmpty(message = "El cobro debe incluir al menos un pago")
        List<CobroItemRequest> pagos,

        @Valid
        CobroDocumentoRequest documento) {
}