package com.restaurante.shared.domain.exception;

/**
 * Credenciales inválidas o sesión expirada. Se traduce a HTTP 401 Unauthorized.
 */
public class UnauthorizedException extends DomainException {

    public UnauthorizedException(String message) {
        super(message);
    }
}