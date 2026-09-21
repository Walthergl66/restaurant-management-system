package com.restaurante.cuentas.domain;

/**
 * Estado de la cuenta de una mesa. Las transiciones viven en una sola clase
 * ({@link Cuenta}): ABIERTA → CERRADA.
 */
public enum EstadoCuenta {
    ABIERTA,
    CERRADA
}