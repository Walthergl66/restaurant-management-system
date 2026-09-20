package com.restaurante.mesas.domain;

/**
 * Estado de una mesa. Las transiciones solo ocurren en {@link Mesa}
 * (regla 1 del negocio: el estado se cambia en una sola clase).
 */
public enum EstadoMesa {
    LIBRE,
    OCUPADA,
    RESERVADA,
    INACTIVA
}