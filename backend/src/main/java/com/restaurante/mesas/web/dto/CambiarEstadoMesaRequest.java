package com.restaurante.mesas.web.dto;

import com.restaurante.mesas.domain.EstadoMesa;
import jakarta.validation.constraints.NotNull;

/**
 * Solicitud para cambiar el estado de una mesa desde administración.
 */
public record CambiarEstadoMesaRequest(
        @NotNull(message = "El estado es obligatorio")
        EstadoMesa estado) {
}