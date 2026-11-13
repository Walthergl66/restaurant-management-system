package com.restaurante.delivery.domain;

/**
 * Estado del recorrido (ruta individual o multientrega) de un repartidor.
 */
public enum EstadoRecorrido {
    /** Planificado pero aún no iniciado. */
    PLANIFICADO,
    /** En curso: el repartidor salió a entregar. */
    EN_CURSO,
    /** Finalizado: todas las paradas resueltas. */
    FINALIZADO,
    /** Cancelado antes de completarse. */
    CANCELADO
}
