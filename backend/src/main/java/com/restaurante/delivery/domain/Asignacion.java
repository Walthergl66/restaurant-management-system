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
 * Asignación de un pedido de cliente a un repartidor. Su estado es LOGÍSTICO
 * (ASIGNADA -> EN_RUTA -> ENTREGADA/FALLIDA, o ASIGNADA -> REASIGNADA) y nunca
 * sobrescribe el estado de preparación del pedido en comercial. Un pedido solo
 * puede tener una asignación activa a la vez (índice único parcial en la BD).
 */
@Entity
@Table(name = "asignaciones", schema = "delivery")
public class Asignacion extends AuditableEntity {

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(name = "pedido_cliente_id", nullable = false)
    private Long pedidoClienteId;

    @Column(name = "repartidor_id", nullable = false)
    private Long repartidorId;

    @Column(name = "recorrido_id")
    private Long recorridoId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoAsignacion estado = EstadoAsignacion.ASIGNADA;

    @Column(name = "asignado_por", length = 50)
    private String asignadoPor;

    @Column(name = "asignado_at", nullable = false)
    private Instant asignadoAt = Instant.now();

    protected Asignacion() {
    }

    public Asignacion(Long pedidoClienteId, Long repartidorId, String asignadoPor) {
        if (pedidoClienteId == null) {
            throw new BusinessRuleException("La asignación debe referir a un pedido");
        }
        if (repartidorId == null) {
            throw new BusinessRuleException("La asignación debe referir a un repartidor");
        }
        this.pedidoClienteId = pedidoClienteId;
        this.repartidorId = repartidorId;
        this.asignadoPor = asignadoPor;
        this.asignadoAt = Instant.now();
    }

    /**
     * Vincula la asignación a un recorrido.
     */
    public void asignarRecorrido(Long recorridoId) {
        this.recorridoId = recorridoId;
    }

    /**
     * Pasa a ruta: solo desde ASIGNADA.
     */
    public void iniciarRuta() {
        if (estado != EstadoAsignacion.ASIGNADA) {
            throw new BusinessRuleException(
                    "Solo una asignación asignada puede salir a ruta");
        }
        this.estado = EstadoAsignacion.EN_RUTA;
    }

    /**
     * Marca entregada: solo desde EN_RUTA.
     */
    public void marcarEntregada() {
        if (estado != EstadoAsignacion.EN_RUTA) {
            throw new BusinessRuleException(
                    "Solo una asignación en ruta puede marcarse entregada");
        }
        this.estado = EstadoAsignacion.ENTREGADA;
    }

    /**
     * Marca fallida: solo desde EN_RUTA.
     */
    public void marcarFallida() {
        if (estado != EstadoAsignacion.EN_RUTA) {
            throw new BusinessRuleException(
                    "Solo una asignación en ruta puede marcarse fallida");
        }
        this.estado = EstadoAsignacion.FALLIDA;
    }

    /**
     * Libera la asignación para reasignar el pedido: solo desde ASIGNADA.
     */
    public void reasignar() {
        if (estado != EstadoAsignacion.ASIGNADA) {
            throw new BusinessRuleException(
                    "Solo una asignación asignada puede reasignarse");
        }
        this.estado = EstadoAsignacion.REASIGNADA;
    }

    public Long getPedidoClienteId() {
        return pedidoClienteId;
    }

    public Long getRepartidorId() {
        return repartidorId;
    }

    public Long getRecorridoId() {
        return recorridoId;
    }

    public EstadoAsignacion getEstado() {
        return estado;
    }

    public String getAsignadoPor() {
        return asignadoPor;
    }

    public Instant getAsignadoAt() {
        return asignadoAt;
    }
}
