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
 * Estado operativo de un repartidor, SEPARADO del rol DELIVERY (seguridad).
 * El rol dice quién puede ser repartidor; esta entidad dice si opera hoy
 * (PENDIENTE/HABILITADO/SUSPENDIDO). Un repartidor que no está HABILITADO
 * no puede recibir asignaciones.
 */
@Entity
@Table(name = "repartidores", schema = "delivery")
public class Repartidor extends AuditableEntity {

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoRepartidor estado = EstadoRepartidor.PENDIENTE;

    @Column(name = "habilitado_por", length = 50)
    private String habilitadoPor;

    @Column(name = "habilitado_at")
    private Instant habilitadoAt;

    protected Repartidor() {
    }

    public Repartidor(Long usuarioId) {
        if (usuarioId == null) {
            throw new BusinessRuleException("El repartidor debe estar ligado a un usuario");
        }
        this.usuarioId = usuarioId;
    }

    /**
     * Habilita a un repartidor PENDIENTE o SUSPENDIDO para que pueda operar.
     */
    public void habilitar(String habilitadoPor) {
        if (estado == EstadoRepartidor.HABILITADO) {
            throw new BusinessRuleException("El repartidor ya está habilitado");
        }
        this.estado = EstadoRepartidor.HABILITADO;
        this.habilitadoPor = habilitadoPor;
        this.habilitadoAt = Instant.now();
    }

    /**
     * Suspende a un repartidor habilitado, dejándolo fuera de ruta.
     */
    public void suspender() {
        if (estado != EstadoRepartidor.HABILITADO) {
            throw new BusinessRuleException(
                    "Solo un repartidor habilitado puede suspenderse");
        }
        this.estado = EstadoRepartidor.SUSPENDIDO;
    }

    public boolean estaHabilitado() {
        return estado == EstadoRepartidor.HABILITADO;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public EstadoRepartidor getEstado() {
        return estado;
    }

    public String getHabilitadoPor() {
        return habilitadoPor;
    }

    public Instant getHabilitadoAt() {
        return habilitadoAt;
    }
}
