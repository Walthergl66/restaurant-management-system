package com.restaurante.delivery.application;

import com.restaurante.delivery.domain.ConfigRestaurante;

import java.math.BigDecimal;

/**
 * Vista de solo lectura de la configuración del restaurante para la capa web:
 * no expone la entidad JPA.
 *
 * <p>{@code requiereSeleccionManual} es true cuando hay dirección pero no
 * coordenadas: el administrador debe marcar el punto en el mapa porque ORS no
 * las resolvió (o la dirección era ambigua).
 */
public record ConfigRestauranteResumen(
        String direccion,
        BigDecimal lat,
        BigDecimal lng,
        String origenCoords,
        boolean tieneCoordenadas,
        boolean requiereSeleccionManual) {

    public static ConfigRestauranteResumen de(ConfigRestaurante config) {
        return new ConfigRestauranteResumen(
                config.getDireccion(),
                config.getLat(),
                config.getLng(),
                config.getOrigenCoords() != null ? config.getOrigenCoords().name() : null,
                config.tieneCoordenadas(),
                !config.tieneCoordenadas());
    }
}
