package com.restaurante.delivery.application;

import java.util.List;

/**
 * Lo que un repartidor HABILITADO ve de su trabajo activo: sus asignaciones
 * vigentes (con datos de entrega y estado logístico) y sus recorridos no
 * finalizados (con sus paradas en orden). Solo lo SUYO.
 */
public record MisEntregas(
        Long repartidorId,
        List<EntregaResumen> asignaciones,
        List<RecorridoResumen> recorridos) {
}
