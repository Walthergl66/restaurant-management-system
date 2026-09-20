package com.restaurante.shared.domain;

import jakarta.persistence.PostLoad;
import jakarta.persistence.Transient;
import org.springframework.data.domain.AfterDomainEventPublication;
import org.springframework.data.domain.DomainEvents;

import java.util.ArrayList;
import java.util.List;

/**
 * Agregado raíz del dominio. Centraliza las reglas de negocio y recolecta
 * {@link DomainEvent} que Spring Data publica en la misma transacción en que
 * se persiste el agregado (base del patrón outbox).
 *
 * <p>No todos los agregados emiten eventos; los demás derivan directamente de
 * {@link AuditableEntity}. Los IDs de negocio que genera la aplicación
 * (pedidos, comandas) se validan contra la base de datos para garantizar
 * unicidad en reintentos.
 */
public abstract class AggregateRoot extends AuditableEntity {

    @Transient
    private final List<DomainEvent> domainEvents = new ArrayList<>();

    protected void registerDomainEvent(DomainEvent event) {
        this.domainEvents.add(event);
    }

    @DomainEvents
    protected List<DomainEvent> domainEvents() {
        return List.copyOf(domainEvents);
    }

    @AfterDomainEventPublication
    protected void clearDomainEvents() {
        this.domainEvents.clear();
    }

    @PostLoad
    protected void discardEventsOnLoad() {
        this.domainEvents.clear();
    }
}