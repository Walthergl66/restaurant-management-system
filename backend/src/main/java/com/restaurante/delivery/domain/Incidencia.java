package com.restaurante.delivery.domain;

import com.restaurante.shared.domain.BaseEntity;
import com.restaurante.shared.domain.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Incidencia de una entrega fallida con su motivo. Solo inserción: el registro
 * se conserva para trazabilidad y es inmutable tras crearse.
 */
@Entity
@Table(name = "incidencias", schema = "delivery")
public class Incidencia extends BaseEntity {

    @Column(name = "parada_id", nullable = false)
    private Long paradaId;

    @Column(nullable = false, length = 300)
    private String motivo;

    @Column(name = "registrado_por", length = 50)
    private String registradoPor;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected Incidencia() {
    }

    public Incidencia(Long paradaId, String motivo, String registradoPor) {
        if (paradaId == null) {
            throw new BusinessRuleException("La incidencia debe referir a una parada");
        }
        if (motivo == null || motivo.isBlank()) {
            throw new BusinessRuleException("La incidencia debe tener un motivo");
        }
        this.paradaId = paradaId;
        this.motivo = motivo;
        this.registradoPor = registradoPor;
        this.createdAt = Instant.now();
    }

    public Long getParadaId() {
        return paradaId;
    }

    public String getMotivo() {
        return motivo;
    }

    public String getRegistradoPor() {
        return registradoPor;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
