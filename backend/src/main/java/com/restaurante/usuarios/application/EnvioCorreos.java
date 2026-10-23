package com.restaurante.usuarios.application;

import com.restaurante.usuarios.CodigoVerificacionGenerado;
import com.restaurante.usuarios.EmisorCorreo;
import com.restaurante.usuarios.RecuperacionPasswordGenerada;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Envía los correos del módulo después de que la transacción confirma.
 *
 * <p>Antes el puerto se invocaba dentro de la transacción: un fallo del
 * proveedor revertía el alta del usuario y la llamada HTTP retenía la
 * transacción de base de datos. Ahora el envío es
 * {@link TransactionPhase#AFTER_COMMIT}: si el registro se revierte no sale
 * ningún correo, y si el proveedor falla la cuenta ya está creada y el usuario
 * puede pedir el reenvío. El fallo se registra sin el código ni el token.
 */
@Component
public class EnvioCorreos {

    private static final Logger log = LoggerFactory.getLogger(EnvioCorreos.class);

    private final EmisorCorreo emisorCorreo;

    public EnvioCorreos(EmisorCorreo emisorCorreo) {
        this.emisorCorreo = emisorCorreo;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void alGenerarCodigo(CodigoVerificacionGenerado evento) {
        enviar("verificación", evento.correo(),
                () -> emisorCorreo.enviarCodigoVerificacion(evento.correo(), evento.codigo()));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void alGenerarRecuperacion(RecuperacionPasswordGenerada evento) {
        enviar("recuperación", evento.correo(),
                () -> emisorCorreo.enviarRecuperacionPassword(
                        evento.correo(), evento.token(), evento.vigencia()));
    }

    /**
     * Un proveedor caído no debe tumbar una operación ya confirmada: se
     * registra el fallo y el usuario conserva la vía de reenvío.
     */
    private void enviar(String tipo, String correo, Runnable envio) {
        try {
            envio.run();
        } catch (RuntimeException e) {
            log.error("No se pudo enviar el correo de {} a '{}'", tipo, correo, e);
        }
    }
}
