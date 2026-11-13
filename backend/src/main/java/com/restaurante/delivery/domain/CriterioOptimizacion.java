package com.restaurante.delivery.domain;

/**
 * Criterio con el que ORS Optimization ordena las paradas de un recorrido.
 */
public enum CriterioOptimizacion {
    /** Minimizar la distancia total recorrida. */
    DISTANCIA,
    /** Minimizar la duración total del recorrido. */
    DURACION
}
