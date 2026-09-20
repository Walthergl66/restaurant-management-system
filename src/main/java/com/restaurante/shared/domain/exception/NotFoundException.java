package com.restaurante.shared.domain.exception;

/**
 * Recurso solicitado que no existe.
 */
public class NotFoundException extends DomainException {

    public NotFoundException(String message) {
        super(message);
    }
}