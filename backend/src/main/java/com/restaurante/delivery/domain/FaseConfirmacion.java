package com.restaurante.delivery.domain;

/**
 * Estado legible de la DOBLE CONFIRMACIÓN de una entrega (parada). Es un
 * concepto SEPARADO del estado logístico de la parada/asignación: deriva de las
 * dos confirmaciones (repartidor y cliente), no de la máquina de estados del
 * recorrido ni del estado de preparación del pedido.
 */
public enum FaseConfirmacion {

    /** Nadie ha confirmado todavía (el repartidor aún no entregó). */
    SIN_CONFIRMAR,

    /** El repartidor confirmó la entrega; falta la confirmación del cliente. */
    PENDIENTE_CONFIRMACION_CLIENTE,

    /** Ambas confirmaciones existen (o el admin la resolvió): entrega verificada. */
    VERIFICADA,

    /** La parada terminó en incidencia (FALLIDA): no hay entrega que verificar. */
    FALLIDA
}
