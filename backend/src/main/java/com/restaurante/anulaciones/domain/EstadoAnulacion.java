package com.restaurante.anulaciones.domain;

/**
 * Estado de una anulación. Es un registro aparte (RF-20 a RF-23): la línea
 * original del pedido nunca se borra ni se edita; SOLICITADA pasa a APROBADA
 * (se descuenta de la cuenta y se imprime la cancelación) o RECHAZADA.
 */
public enum EstadoAnulacion {
    SOLICITADA,
    APROBADA,
    RECHAZADA
}