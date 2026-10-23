package com.restaurante.delivery.domain;

/**
 * De dónde salieron las coordenadas vigentes de la ubicación del restaurante.
 *
 * <ul>
 *   <li>{@code GEOCODING}: resueltas por ORS a partir del texto de la dirección.</li>
 *   <li>{@code MANUAL}: el administrador las marcó en el mapa.</li>
 * </ul>
 *
 * <p>La ausencia de valor (null en la entidad) significa que aún no hay
 * coordenadas: la configuración está pendiente de selección manual.
 */
public enum OrigenCoords {
    GEOCODING,
    MANUAL
}
