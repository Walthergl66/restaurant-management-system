package com.restaurante.cuentas;

import java.math.BigDecimal;

/**
 * Vista de una cuenta para otros módulos (pagos, reportes...). El total ya
 * viene calculado desde los registros (pedidos confirmados − anulaciones
 * aprobadas, RNF-16) por la clase de cálculo de cuentas.
 */
public record CuentaResumen(
        Long id,
        Long mesaId,
        int numeroMesa,
        String estado,
        BigDecimal total) {
}