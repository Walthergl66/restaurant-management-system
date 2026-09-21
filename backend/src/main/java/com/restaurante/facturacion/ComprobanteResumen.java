package com.restaurante.facturacion;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Vista de un comprobante emitido para otros módulos y para la API.
 */
public record ComprobanteResumen(
        Long id,
        String correlativo,
        Long secuencial,
        Long cuentaId,
        String tipo,
        BigDecimal total,
        String clienteNombre,
        String clienteIdentificacion,
        String emitidoPor,
        Instant fecha) {
}