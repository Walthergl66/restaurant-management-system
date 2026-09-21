package com.restaurante.mesas;

import com.restaurante.mesas.domain.EstadoMesa;

/**
 * Vista de la mesa para otros módulos.
 */
public record MesaResumen(
        Long id,
        int numero,
        EstadoMesa estado,
        boolean activa) {
}