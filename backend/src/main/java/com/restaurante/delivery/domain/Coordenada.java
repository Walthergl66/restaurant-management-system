package com.restaurante.delivery.domain;

import java.math.BigDecimal;

/**
 * Par de coordenadas geográficas (latitud, longitud) en grados decimales.
 *
 * <p>ORS trabaja con el orden {@code [lng, lat]} en los cuerpos de petición;
 * aquí se guarda el orden humano (lat, lng) y la conversión al formato de ORS
 * se hace en el cliente de infraestructura, no aquí.
 */
public record Coordenada(BigDecimal lat, BigDecimal lng) {

    public Coordenada {
        if (lat == null || lng == null) {
            throw new IllegalArgumentException("Una coordenada necesita latitud y longitud");
        }
    }

    public static Coordenada of(double lat, double lng) {
        return new Coordenada(BigDecimal.valueOf(lat), BigDecimal.valueOf(lng));
    }
}
