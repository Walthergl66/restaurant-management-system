package com.restaurante.usuarios;

import java.time.Duration;

/**
 * Evento de dominio: se generó un token de recuperación de contraseña (RF-45).
 * Se publica dentro de la transacción que guarda el token; el correo sale solo
 * si esa transacción confirma (ver {@code EnvioCorreos}).
 */
public record RecuperacionPasswordGenerada(String correo, String token, Duration vigencia) {
}
