package com.restaurante.shared.outbox;

/**
 * Estado de una orden del outbox: pendiente de entregar, entregada o fallida
 * (para reintento).
 */
public enum EstadoOutbox {
    PENDIENTE,
    ENVIADO,
    FALLIDO
}