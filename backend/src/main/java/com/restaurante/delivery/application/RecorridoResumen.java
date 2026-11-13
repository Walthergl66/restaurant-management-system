package com.restaurante.delivery.application;

import com.restaurante.delivery.domain.Coordenada;
import com.restaurante.delivery.domain.Recorrido;

import java.math.BigDecimal;
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
        BigDecimal origenLat,
        BigDecimal origenLng,
        List<ParadaResumen> paradas) {

    static RecorridoResumen de(Recorrido r, Coordenada origen, List<ParadaResumen> paradas) {
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
                origen != null ? origen.lat() : null,
                origen != null ? origen.lng() : null,
                paradas);
    }
}
