package com.restaurante.shared.domain.exception;

/**
 * Intento de ejecutar una operación prohibida por una regla del negocio
 * (por ejemplo editar un pedido ya confirmado).
 */
public class BusinessRuleException extends DomainException {

    public BusinessRuleException(String message) {
        super(message);
    }
}