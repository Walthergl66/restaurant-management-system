package com.restaurante.comandas.web.dto;

import com.restaurante.shared.outbox.EventoOutbox;

import java.time.Instant;

/**
 * Orden de impresión pendiente para el agente local (opción 1 del plan §4).
 */
public record ImpresionResponse(
        Long id,
        String pedidoCodigo,
        Integer numeroComanda,
        Long areaId,
        String areaNombre,
        String evento,
        int intentos,
        Instant creadaAt) {

    public static ImpresionResponse from(EventoOutbox evento) {
        return new ImpresionResponse(
                evento.getId(),
                evento.getPedidoCodigo(),
                evento.getNumeroComanda(),
                evento.getAreaId(),
                evento.getAreaNombre(),
                evento.getEvento(),
                evento.getIntentos(),
                evento.getCreadaAt());
    }
}