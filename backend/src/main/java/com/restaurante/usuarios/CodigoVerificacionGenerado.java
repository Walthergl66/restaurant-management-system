package com.restaurante.usuarios;

/**
 * Evento de dominio: se generó un código de verificación de correo (RF-45).
 * Se publica dentro de la transacción que guarda el código; el envío real
 * ocurre solo si esa transacción confirma (ver {@code EnvioCorreos}).
 */
public record CodigoVerificacionGenerado(String correo, String codigo) {
}
