package com.restaurante.delivery.domain;

/**
 * Estado de una parada dentro de un recorrido.
 */
public enum EstadoParada {
    /** Aún sin resolver. */
    PENDIENTE,
    /** Pedido entregado en esta parada. */
    ENTREGADA,
    /** Intento de entrega fallido en esta parada. */
    FALLIDA
}
