package com.restaurante.usuarios;

/**
 * El proveedor de correo no aceptó el envío o no se pudo contactar con él.
 * <p>
 * Es una señal de fallo del canal, nunca del negocio: quien la recibe (el
 * envío va tras el commit, Fase 3) debe registrar el incidente y seguir, sin
 * revertir el registro del usuario. El mensaje no contiene nunca el código de
 * verificación ni la clave API.
 */
public class CorreoNoEnviadoException extends RuntimeException {

    public CorreoNoEnviadoException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
