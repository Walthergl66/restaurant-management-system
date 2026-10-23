package com.restaurante.delivery.domain;

/**
 * Estado operativo del repartidor, SEPARADO del rol DELIVERY de seguridad.
 * El rol dice quién puede ser repartidor; este estado dice si opera hoy.
 */
public enum EstadoRepartidor {
    /** Registrado pero aún sin habilitar: no puede recibir asignaciones. */
    PENDIENTE,
    /** Habilitado por administración: puede recibir asignaciones y operar. */
    HABILITADO,
    /** Suspendido: queda fuera de ruta sin borrarse. */
    SUSPENDIDO
}
