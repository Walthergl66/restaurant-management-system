package com.restaurante.comandas.domain;

import com.restaurante.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Orden de impresión en el outbox. Se escribe en la MISMA transacción que la
 * confirmación del pedido, así si el agente local se cae, la orden queda
 * pendiente y se reenvía (al menos una vez).
 */
@Entity
@Table(name = "outbox")
public class EventoOutbox extends BaseEntity {

    @Column(nullable = false, length = 40)
    private String tipo;

    @Column(name = "agregado_id", nullable = false, length = 64)
    private String agregadoId;

    @Column(name = "pedido_codigo", length = 40)
    private String pedidoCodigo;

    @Column(name = "numero_comanda")
    private Integer numeroComanda;

    @Column(name = "area_id")
    private Long areaId;

    @Column(name = "area_nombre", length = 80)
    private String areaNombre;

    @Column(nullable = false, length = 80)
    private String evento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoOutbox estado = EstadoOutbox.PENDIENTE;

    @Column(nullable = false)
    private int intentos;

    @Column(length = 500)
    private String error;

    @Column(name = "creada_at", nullable = false, updatable = false)
    private Instant creadaAt;

    @Column(name = "procesada_at")
    private Instant procesadaAt;

    protected EventoOutbox() {
    }

    public EventoOutbox(String tipo, String agregadoId, String pedidoCodigo,
                        Integer numeroComanda, Long areaId, String areaNombre, String evento) {
        this.tipo = tipo;
        this.agregadoId = agregadoId;
        this.pedidoCodigo = pedidoCodigo;
        this.numeroComanda = numeroComanda;
        this.areaId = areaId;
        this.areaNombre = areaNombre;
        this.evento = evento;
    }

    @PrePersist
    void prePersist() {
        this.creadaAt = Instant.now();
    }

    public void marcarEnviado() {
        this.estado = EstadoOutbox.ENVIADO;
        this.procesadaAt = Instant.now();
    }

    public void marcarFallido(String motivo) {
        this.estado = EstadoOutbox.FALLIDO;
        this.intentos++;
        this.error = motivo;
    }

    public String getTipo() {
        return tipo;
    }

    public String getAgregadoId() {
        return agregadoId;
    }

    public String getPedidoCodigo() {
        return pedidoCodigo;
    }

    public Integer getNumeroComanda() {
        return numeroComanda;
    }

    public Long getAreaId() {
        return areaId;
    }

    public String getAreaNombre() {
        return areaNombre;
    }

    public String getEvento() {
        return evento;
    }

    public EstadoOutbox getEstado() {
        return estado;
    }

    public int getIntentos() {
        return intentos;
    }

    public String getError() {
        return error;
    }

    public Instant getCreadaAt() {
        return creadaAt;
    }

    public Instant getProcesadaAt() {
        return procesadaAt;
    }
}