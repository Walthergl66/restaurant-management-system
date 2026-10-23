package com.restaurante.delivery.application;

/**
 * Vista para el repartidor de una de sus asignaciones activas con los datos de
 * entrega del pedido (PII) y su estado logístico. El repartidor solo ve lo
 * suyo (la propiedad se verifica en backend, no en la UI).
 */
public record EntregaResumen(
        Long asignacionId,
        Long pedidoClienteId,
        Long recorridoId,
        String estadoAsignacion,
        String codigo,
        String clienteNombre,
        String direccion,
        String telefono,
        String referencia) {
}
