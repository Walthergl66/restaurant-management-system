package com.restaurante.usuarios.application;

import com.restaurante.usuarios.CodigoVerificacionPendiente;
import com.restaurante.usuarios.EmisorCorreo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Envía el código de verificación (RF-45) por {@link EmisorCorreo} una vez
 * confirmada la transacción que lo persistió (RNF-11): si el proveedor falla,
 * la cuenta ya quedó registrada y el HTTP respondió bien; el fallo se
 * registra en el log y el usuario puede reintentar con "reenviar
 * verificación". Cualquier excepción se captura a propósito: después del
 * commit no hay vuelta atrás y un error aquí no debe tumbar la respuesta.
 * El log nunca incluye el código (RNF-10).
 */
@Component
public class EnviadorCodigoVerificacion {

    private static final Logger log = LoggerFactory.getLogger(EnviadorCodigoVerificacion.class);

    private final EmisorCorreo emisorCorreo;

    public EnviadorCodigoVerificacion(EmisorCorreo emisorCorreo) {
        this.emisorCorreo = emisorCorreo;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCodigoVerificacionPendiente(CodigoVerificacionPendiente evento) {
        try {
            emisorCorreo.enviarCodigoVerificacion(evento.correo(), evento.codigo());
        } catch (Exception e) {
            log.error("No se pudo enviar el código de verificación a '{}': {}",
                    evento.correo(), e.getMessage(), e);
        }
    }
}
