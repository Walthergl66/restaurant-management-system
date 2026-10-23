package com.restaurante.usuarios.infrastructure;

import com.restaurante.usuarios.EmisorCorreo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Implementación de {@link EmisorCorreo} para desarrollo y pruebas: escribe el
 * correo en el log del servidor en vez de enviarlo. Se activa mientras
 * {@code app.correo.brevo.habilitado} no sea {@code true}; con Brevo encendido
 * el bean lo reemplaza {@code EmisorCorreoBrevo}. Nunca expone el código en la
 * respuesta HTTP.
 */
@Component
@ConditionalOnProperty(prefix = "app.correo.brevo", name = "habilitado",
        havingValue = "false", matchIfMissing = true)
public class EmisorCorreoLog implements EmisorCorreo {

    private static final Logger log = LoggerFactory.getLogger(EmisorCorreoLog.class);

    @Override
    public void enviarCodigoVerificacion(String correo, String codigo) {
        log.info("Correo de verificación para '{}'. Código (entrega manual en dev): {}",
                correo, codigo);
    }

    @Override
    public void enviarRecuperacionPassword(String correo, String token, Duration vigencia) {
        log.warn("Correo de recuperación para '{}'. Token válido {} min (entrega manual en dev): {}",
                correo, vigencia.toMinutes(), token);
    }
}
