package com.restaurante.usuarios;

/**
 * Evento publicado (misma transacción) cuando se persiste un código de
 * verificación de correo de una cuenta nueva o en reenvío (RF-45). El envío
 * real por el proveedor ocurre después del commit (RNF-11): un fallo del
 * canal de correo no revierte el registro y el usuario puede reintentar con
 * "reenviar verificación". El código viaja solo en el evento, que no se
 * registra en logs ni en auditoría (RNF-10).
 */
public record CodigoVerificacionPendiente(
        Long usuarioId,
        String correo,
        String codigo) {
}
