package com.restaurante.delivery.application;

import com.restaurante.delivery.domain.Recorrido;

import java.time.Instant;
import java.util.List;

/**
 * Vista de un recorrido del repartidor con su estado logístico, totales y las
 * paradas en orden de visita. No expone la entidad JPA.
 */
public record RecorridoResumen(
        Long recorridoId,
        Long repartidorId,
        String tipo,
        String estado,
        String criterio,
        Integer distanciaTotalM,
        Integer duracionTotalS,
        Instant iniciadoAt,
        Instant finalizadoAt,
        List<ParadaResumen> paradas) {

    static RecorridoResumen de(Recorrido r, List<ParadaResumen> paradas) {
        return new RecorridoResumen(
                r.getId(),
                r.getRepartidorId(),
                r.getTipo().name(),
                r.getEstado().name(),
                r.getCriterioOptimizacion().name(),
                r.getDistanciaTotalM(),
                r.getDuracionTotalS(),
                r.getIniciadoAt(),
                r.getFinalizadoAt(),
                paradas);
    }
}
