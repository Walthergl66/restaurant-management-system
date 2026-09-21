package com.restaurante.caja.domain;

/**
 * Estado de una caja. Las transiciones viven en una sola clase
 * ({@link Caja}): ABIERTA → CERRADA.
 */
public enum EstadoCaja {
    ABIERTA,
    CERRADA
}