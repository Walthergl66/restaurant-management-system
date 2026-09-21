package com.restaurante.facturacion;

import java.math.BigDecimal;

/**
 * Orden de emisión de un comprobante. {@code tipo} es una cadena
 * ({@code FACTURA} o {@code TICKET}); los datos del cliente son opcionales.
 */
public record EmitirComprobante(
        Long cuentaId,
        String tipo,
        BigDecimal total,
        String clienteNombre,
        String clienteIdentificacion,
        String emitidoPor) {
}