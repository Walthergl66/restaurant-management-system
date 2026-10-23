package com.restaurante.usuarios.infrastructure;

import com.restaurante.usuarios.EmisorCorreo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Implementación por defecto de {@link EmisorCorreo}: escribe el correo en el
 * log del servidor (entrega manual en desarrollo). Es el adaptador activo
 * mientras {@code MAIL_ENABLED} no esté en {@code true}; cuando se enciende,
 * el rol pasa a {@link EmisorCorreoResend} y este bean no se monta (los dos
 * son excluyentes: siempre hay exactamente uno, nunca dos).
 * <p>
 * Nunca expone el código en la respuesta HTTP; solo en el log, que es la
 * entrega en este modo.
 */
@Component
@ConditionalOnProperty(prefix = "app.mail", name = "enabled",
        havingValue = "false", matchIfMissing = true)
public class EmisorCorreoLog implements EmisorCorreo {

    private static final Logger log = LoggerFactory.getLogger(EmisorCorreoLog.class);

    @Override
    public void enviarCodigoVerificacion(String correo, String codigo) {
        log.info("Correo de verificación para '{}'. Código (entrega manual en dev): {}",
                correo, codigo);
    }
}
