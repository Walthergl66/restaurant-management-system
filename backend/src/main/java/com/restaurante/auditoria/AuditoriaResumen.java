package com.restaurante.auditoria;

import java.time.Instant;

/**
 * Vista de un evento de auditoría para la consulta (RF-52). La tabla es de
 * solo inserción: aquí nunca se edita nada.
 */
public record AuditoriaResumen(
        Long id,
        String usuario,
        String tipo,
        String entidad,
        String entidadId,
        String detalle,
        Instant fecha) {
}