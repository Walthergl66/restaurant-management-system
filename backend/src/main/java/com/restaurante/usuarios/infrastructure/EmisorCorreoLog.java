package com.restaurante.usuarios.infrastructure;

import com.restaurante.usuarios.EmisorCorreo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Implementación por defecto de {@link EmisorCorreo}: escribe el correo en el
 * log del servidor (no hay SMTP en este alcance). Nunca expone el código en la
 * respuesta HTTP.
 */
@Component
public class EmisorCorreoLog implements EmisorCorreo {

    private static final Logger log = LoggerFactory.getLogger(EmisorCorreoLog.class);

    @Override
    public void enviarCodigoVerificacion(String correo, String codigo) {
        log.info("Correo de verificación para '{}'. Código (entrega manual en dev): {}",
                correo, codigo);
    }
}
