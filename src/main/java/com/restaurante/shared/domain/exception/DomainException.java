package com.restaurante.shared.domain.exception;

/**
 * Base de todas las excepciones de negocio del sistema.
 */
public abstract class DomainException extends RuntimeException {

    protected DomainException(String message) {
        super(message);
    }

    protected DomainException(String message, Throwable cause) {
        super(message, cause);
    }
}