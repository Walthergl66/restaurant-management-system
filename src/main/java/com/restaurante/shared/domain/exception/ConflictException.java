package com.restaurante.shared.domain.exception;

/**
 * Conflicto de concurrencia o de unicidad: reintento duplicado, estado no
 * esperado, registros en paralelo. Se traduce a HTTP 409 Conflict.
 */
public class ConflictException extends DomainException {

    public ConflictException(String message) {
        super(message);
    }
}