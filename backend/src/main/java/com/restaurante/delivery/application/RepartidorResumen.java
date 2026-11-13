package com.restaurante.delivery.application;

import com.restaurante.delivery.domain.EstadoRepartidor;
import com.restaurante.delivery.domain.Repartidor;

import java.time.Instant;

/**
 * Vista de solo lectura de un repartidor para la capa web: no expone la
 * entidad JPA.
 */
public record RepartidorResumen(
        Long id,
        Long usuarioId,
        EstadoRepartidor estado,
        String habilitadoPor,
        Instant habilitadoAt) {

    public static RepartidorResumen de(Repartidor repartidor) {
        return new RepartidorResumen(
                repartidor.getId(),
                repartidor.getUsuarioId(),
                repartidor.getEstado(),
                repartidor.getHabilitadoPor(),
                repartidor.getHabilitadoAt());
    }
}
