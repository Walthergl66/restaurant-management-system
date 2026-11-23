package com.restaurante.mesas.web.dto;

import com.restaurante.mesas.application.ZonaVista;

/** Vista de una zona del restaurante. */
public record ZonaResponse(Long id, String nombre, int orden, long cantidadMesas) {

    public static ZonaResponse from(ZonaVista zona) {
        return new ZonaResponse(zona.id(), zona.nombre(), zona.orden(), zona.cantidadMesas());
    }
}
