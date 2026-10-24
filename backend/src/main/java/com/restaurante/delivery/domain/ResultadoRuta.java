package com.restaurante.delivery.domain;

/**
 * Resumen de una ruta calculada por ORS Directions: la geometría codificada
 * (polyline o GeoJSON según el endpoint), la distancia total en metros y la
 * duración total en segundos.
 */
public record ResultadoRuta(String geometria, int distanciaM, int duracionS) {
}
