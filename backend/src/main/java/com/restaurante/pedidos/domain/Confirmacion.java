package com.restaurante.pedidos.domain;

import com.restaurante.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;

/**
 * Prueba de confirmación. La restricción única (pedido, idempotency_key)
 * garantiza que reintentar confirmar no duplique la comanda ni la impresión.
 */
@Entity
@Table(name = "confirmaciones")
public class Confirmacion extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pedido_id", nullable = false)
    private Pedido pedido;

    @Column(name = "idempotency_key", nullable = false, length = 100)
    private String idempotencyKey;

    @Column(name = "confirmado_at", nullable = false, updatable = false)
    private Instant confirmadoAt;

    protected Confirmacion() {
    }

    public Confirmacion(Pedido pedido, String idempotencyKey) {
        this.pedido = pedido;
        this.idempotencyKey = idempotencyKey;
    }

    @PrePersist
    void prePersist() {
        if (confirmadoAt == null) {
            confirmadoAt = Instant.now();
        }
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public Instant getConfirmadoAt() {
        return confirmadoAt;
    }

    public boolean tieneLaMismaClave(String idempotencyKey) {
        return Objects.equals(this.idempotencyKey, idempotencyKey);
    }
}