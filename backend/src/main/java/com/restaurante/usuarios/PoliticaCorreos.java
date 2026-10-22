package com.restaurante.usuarios;

import java.time.Duration;

/**
 * Vigencias de los códigos que viajan por correo (RF-45). Viven en un solo
 * lugar para que el texto del correo ("válido por N minutos") y la validación
 * del backend no puedan desincronizarse.
 */
public final class PoliticaCorreos {

    /** Vigencia del código de verificación de correo. */
    public static final Duration VERIFICACION = Duration.ofMinutes(30);

    /** Vigencia del token de recuperación de contraseña. */
    public static final Duration RECUPERACION = Duration.ofMinutes(30);

    private PoliticaCorreos() {
    }
}
