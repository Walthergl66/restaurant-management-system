package com.restaurante.pagos.domain;

/**
 * Métodos de pago aceptados (RF-26, RF-31). La app del cliente solo elige uno
 * (sin pasarela por ahora); el cobro presencial admite pagos mixtos.
 */
public enum MetodoPago {
    EFECTIVO,
    TARJETA,
    TRANSFERENCIA,
    OTRO
}