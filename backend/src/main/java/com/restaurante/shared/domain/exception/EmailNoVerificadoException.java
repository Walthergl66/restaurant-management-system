package com.restaurante.shared.domain.exception;

/**
 * La cuenta existe y las credenciales son correctas, pero el correo aún no fue
 * verificado (RF-45). Se traduce a HTTP 403 con un tipo de problema propio para
 * que la app móvil pueda dirigir al usuario a la pantalla de verificación.
 */
public class EmailNoVerificadoException extends DomainException {

    public EmailNoVerificadoException(String message) {
        super(message);
    }
}
