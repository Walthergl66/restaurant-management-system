package com.restaurante.pagos;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * API pública del módulo de pagos para lectura de finanzas y reportes
 * (sin acoplarse al módulo por Spring Modulith).
 */
public interface Pagos {

    /**
     * Pagos registrados entre dos instantes (para el resumen de finanzas y
     * los reportes de ventas, RF-36 a RF-39, RF-51).
     */
    List<PagoRegistro> pagosEnPeriodo(Instant desde, Instant hasta);

    record PagoRegistro(Long cuentaId, String metodo, BigDecimal monto, Instant fecha) {
    }
}