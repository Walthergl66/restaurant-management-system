package com.restaurante.delivery.domain;

/**
 * Estado LOGÍSTICO de la asignación de un pedido a un repartidor. No toca
 * ni sobrescribe el estado de preparación del pedido en comercial.
 */
public enum EstadoAsignacion {
    /** Recién asignada, aún sin salir a ruta. */
    ASIGNADA,
    /** El repartidor ya salió con el pedido. */
    EN_RUTA,
    /** Entregada al cliente. */
    ENTREGADA,
    /** Intento de entrega fallido. */
    FALLIDA,
    /** Liberada para reasignar a otro repartidor. */
    REASIGNADA
}
