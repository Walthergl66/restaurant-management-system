package com.restaurante.pagos;

import java.math.BigDecimal;
import java.util.List;

/**
 * Evento publicado al cobrar una cuenta en la MISMA transacción. Lo consumen
 * finanzas/reportes (Fase 6) y las notificaciones al cliente (Fase 8) sin
 * acoplarse al módulo de pagos.
 */
public record CuentaCobrada(
        Long cuentaId,
        Long mesaId,
        BigDecimal total,
        Long cajaId,
        String comprobanteCorrelativo,
        List<PagoResumen> pagos) {

    public record PagoResumen(Long id, String metodo, BigDecimal monto) {
    }
}