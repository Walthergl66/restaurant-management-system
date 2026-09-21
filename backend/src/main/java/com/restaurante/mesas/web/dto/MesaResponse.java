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
        String ubicacion,
        EstadoMesa estado,
        boolean activo) {

    public static MesaResponse from(Mesa mesa) {
        return new MesaResponse(
                mesa.getId(),
                mesa.getNumero(),
                mesa.getCapacidad(),
                mesa.getUbicacion(),
                mesa.getEstado(),
                mesa.isActivo());
    }
}