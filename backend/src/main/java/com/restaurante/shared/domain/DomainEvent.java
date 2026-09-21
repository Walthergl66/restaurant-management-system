package com.restaurante.shared.domain;

import java.time.Instant;

/**
 * Evento de dominio transportado por un {@link AggregateRoot}.
 */
public abstract class DomainEvent {

    private final Instant occurredOn = Instant.now();

    public Instant getOccurredOn() {
        return occurredOn;
    }
}