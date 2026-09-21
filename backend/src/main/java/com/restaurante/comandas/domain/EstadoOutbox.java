package com.restaurante.comandas.domain;

/**
 * Estados de una orden de impresión en el outbox.
 */
public enum EstadoOutbox {
    PENDIENTE,
    ENVIADO,
    FALLIDO
}