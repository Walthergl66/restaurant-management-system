package com.restaurante.mesas.application;

/** Zona con el número de mesas que tiene (la capa web la convierte en su respuesta). */
public record ZonaVista(Long id, String nombre, int orden, long cantidadMesas) {
}
