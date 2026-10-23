package com.restaurante.delivery.application;

import java.util.List;

/**
 * Propuesta de recorrido multientrega: el ORDEN eficiente de paradas que ORS
 * Optimization (VROOM) devolvió, numerado, junto a la geometría por carretera
 * de ese orden y los totales de distancia/duración.
 *
 * <p>Es una PROPUESTA: no persiste nada. El repartidor la materializa con
 * {@code crearRecorrido} pasando {@code asignacionIdsEnOrden} en este mismo
 * orden. No afirma optimalidad matemática: es el mejor orden que VROOM resolvió
 * para los destinos dados. Si VROOM no pudo asignar todas las paradas, el
 * servicio informa el problema (503) y no devuelve una propuesta parcial.
 */
public record PropuestaMultientrega(
        List<ParadaPropuesta> paradas,
        String criterio,
        String geometria,
        int distanciaTotalM,
        int duracionTotalS) {

    /** Una parada de la propuesta en su orden de visita (0-based). */
    public record ParadaPropuesta(
            int orden,
            Long asignacionId,
            Long pedidoClienteId,
            String codigo,
            String direccion) {
    }

    /** Lista de asignaciones en el orden propuesto, lista para crearRecorrido. */
    public List<Long> asignacionIdsEnOrden() {
        return paradas.stream().map(ParadaPropuesta::asignacionId).toList();
    }
}
