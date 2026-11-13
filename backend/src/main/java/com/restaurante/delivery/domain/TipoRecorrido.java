package com.restaurante.delivery.domain;

/**
 * Tipo de recorrido según el número de paradas que agrupa.
 */
public enum TipoRecorrido {
    /** Una sola entrega. */
    INDIVIDUAL,
    /** Varias entregas con orden optimizado por ORS. */
    MULTIPLE
}
