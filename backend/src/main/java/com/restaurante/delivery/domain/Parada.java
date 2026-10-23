package com.restaurante.delivery.domain;

import com.restaurante.shared.domain.AuditableEntity;
import com.restaurante.shared.domain.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;

/**
 * Parada ordenada dentro de un recorrido. El orden es 0-based (la primera es 0)
 * y lo decide ORS Optimization en fases posteriores. Cada parada resuelve la
 * asignación (pedido) a la que apunta; marcar entregada/fallida exige que esté
 * PENDIENTE para evitar la doble confirmación.
 */
@Entity
@Table(name = "paradas", schema = "delivery")
public class Parada extends AuditableEntity {

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(name = "recorrido_id", nullable = false)
    private Long recorridoId;

    @Column(name = "asignacion_id", nullable = false)
    private Long asignacionId;

    @Column(nullable = false)
    private Integer orden;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoParada estado = EstadoParada.PENDIENTE;

    @Column(name = "entregado_at")
    private Instant entregadoAt;

    protected Parada() {
    }

    public Parada(Long recorridoId, Long asignacionId, Integer orden) {
        if (recorridoId == null) {
            throw new BusinessRuleException("La parada debe pertenecer a un recorrido");
        }
        if (asignacionId == null) {
            throw new BusinessRuleException("La parada debe referir a una asignación");
        }
        if (orden == null || orden < 0) {
            throw new BusinessRuleException("El orden de la parada no puede ser negativo");
        }
        this.recorridoId = recorridoId;
        this.asignacionId = asignacionId;
        this.orden = orden;
    }

    /**
     * Marca la parada como entregada: solo si está PENDIENTE (evita doble
     * confirmación).
     */
    public void marcarEntregada() {
        requierePendiente();
        this.estado = EstadoParada.ENTREGADA;
        this.entregadoAt = Instant.now();
    }

    /**
     * Marca la parada como fallida: solo si está PENDIENTE.
     */
    public void marcarFallida() {
        requierePendiente();
        this.estado = EstadoParada.FALLIDA;
    }

    private void requierePendiente() {
        if (estado != EstadoParada.PENDIENTE) {
            throw new BusinessRuleException(
                    "Una parada " + estado.name().toLowerCase() + " no admite otra confirmación");
        }
    }

    public Long getRecorridoId() {
        return recorridoId;
    }

    public Long getAsignacionId() {
        return asignacionId;
    }

    public Integer getOrden() {
        return orden;
    }

    public EstadoParada getEstado() {
        return estado;
    }

    public Instant getEntregadoAt() {
        return entregadoAt;
    }
}
