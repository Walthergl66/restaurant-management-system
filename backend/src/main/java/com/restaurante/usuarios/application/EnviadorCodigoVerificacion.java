package com.restaurante.usuarios.application;

import com.restaurante.auditoria.Auditoria;
import com.restaurante.usuarios.CodigoVerificacionPendiente;
import com.restaurante.usuarios.EmisorCorreo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import tools.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Envía el código de verificación (RF-45) por {@link EmisorCorreo} una vez
 * confirmada la transacción que lo persistió (RNF-11): si el proveedor falla,
 * la cuenta ya quedó registrada y el HTTP respondió bien; el fallo se
 * registra en el log y el usuario puede reintentar con "reenviar
 * verificación". Cualquier excepción del envío se captura a propósito:
 * después del commit no hay vuelta atrás y un error aquí no debe tumbar la
 * respuesta.
 * <p>
 * Cada intento queda en el historial de auditoría con categoría SEGURIDAD
 * (RNF-10): el detalle lleva el correo y, si falla, el motivo del canal —
 * nunca el código de un solo uso. Se audita por la API pública
 * {@link Auditoria} (mismo patrón que UsuarioService) para no crear una
 * dependencia auditoría→usuarios que Modulith rechazaría por ciclo.
 */
@Component
public class EnviadorCodigoVerificacion {

    /** Límite del motivo copiado a auditoría; sobra para diagnosticar. */
    private static final int MOTIVO_MAX = 200;

    private static final Logger log = LoggerFactory.getLogger(EnviadorCodigoVerificacion.class);

    private final EmisorCorreo emisorCorreo;
    private final Auditoria auditoria;
    private final ObjectMapper objectMapper;

    public EnviadorCodigoVerificacion(EmisorCorreo emisorCorreo,
                                      Auditoria auditoria,
                                      ObjectMapper objectMapper) {
        this.emisorCorreo = emisorCorreo;
        this.auditoria = auditoria;
        this.objectMapper = objectMapper;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCodigoVerificacionPendiente(CodigoVerificacionPendiente evento) {
        String motivoFallo = null;
        try {
            emisorCorreo.enviarCodigoVerificacion(evento.correo(), evento.codigo());
        } catch (Exception e) {
            motivoFallo = recortar(e.getMessage());
            log.error("No se pudo enviar el código de verificación a '{}': {}",
                    evento.correo(), e.getMessage(), e);
        }
        if (motivoFallo == null) {
            auditoria.registrar("CORREO_VERIFICACION_ENVIADO", "USUARIO",
                    String.valueOf(evento.usuarioId()),
                    json(Map.of("correo", evento.correo())), "SEGURIDAD");
        } else {
            Map<String, String> detalle = new LinkedHashMap<>();
            detalle.put("correo", evento.correo());
            detalle.put("motivo", motivoFallo);
            auditoria.registrar("CORREO_VERIFICACION_FALLIDO", "USUARIO",
                    String.valueOf(evento.usuarioId()), json(detalle), "SEGURIDAD");
        }
    }

    private String json(Map<String, String> detalle) {
        try {
            return objectMapper.writeValueAsString(detalle);
        } catch (Exception e) {
            return "{\"detalle\":\"no serializable\"}";
        }
    }

    private static String recortar(String motivo) {
        if (motivo == null || motivo.isBlank()) {
            return "sin detalle";
        }
        return motivo.length() > MOTIVO_MAX ? motivo.substring(0, MOTIVO_MAX) + "..." : motivo;
    }
}
