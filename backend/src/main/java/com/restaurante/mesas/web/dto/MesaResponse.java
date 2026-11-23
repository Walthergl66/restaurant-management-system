package com.restaurante.mesas.web.dto;

import com.restaurante.mesas.domain.EstadoMesa;
import com.restaurante.mesas.domain.Mesa;

/**
 * Vista de una mesa.
 */
public record MesaResponse(
        Long id,
        int numero,
        int capacidad,
        /** Nombre de la zona (se conserva por compatibilidad con los clientes). */
        String ubicacion,
        Long zonaId,
        int zonaOrden,
        EstadoMesa estado,
        boolean activo) {

    public static MesaResponse from(Mesa mesa) {
        return new MesaResponse(
                mesa.getId(),
                mesa.getNumero(),
                mesa.getCapacidad(),
                mesa.getZona().getNombre(),
                mesa.getZona().getId(),
                mesa.getZona().getOrden(),
                mesa.getEstado(),
                mesa.isActivo());
    }
}