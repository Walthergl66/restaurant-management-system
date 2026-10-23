package com.restaurante.delivery.application;

import com.restaurante.delivery.domain.Parada;

/**
 * Vista de una parada dentro de un recorrido para el repartidor: su orden de
 * visita (0-based), el estado logístico de la parada y la asignación (pedido)
 * que resuelve.
 */
public record ParadaResumen(
        Long paradaId,
        Long asignacionId,
        Long pedidoClienteId,
        int orden,
        String estado,
        String codigo,
        String direccion) {

    static ParadaResumen de(Parada parada, EntregaResumen entrega) {
        return new ParadaResumen(
                parada.getId(),
                parada.getAsignacionId(),
                entrega != null ? entrega.pedidoClienteId() : null,
                parada.getOrden(),
                parada.getEstado().name(),
                entrega != null ? entrega.codigo() : null,
                entrega != null ? entrega.direccion() : null);
    }
}
