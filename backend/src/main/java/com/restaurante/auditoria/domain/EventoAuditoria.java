package com.restaurante.auditoria.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Evento de auditoría. Mapa a {@code auditoria_eventos}, una tabla de SOLO
 * INSERCIÓN (RNF-10): no hay actualización ni borrado desde la aplicación.
 */
@Entity
@Table(name = "auditoria_eventos")
public class EventoAuditoria {

    @jakarta.persistence.Id
    @jakarta.persistence.GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String usuario;

    @Column(nullable = false, length = 40)
    private String tipo;

    @Column(nullable = false, length = 40)
    private String entidad;

    @Column(name = "entidad_id", length = 50)
    private String entidadId;

    @Column(columnDefinition = "jsonb")
    private String detalle;

    @Column(nullable = false)
    private Instant fecha = Instant.now();

    protected EventoAuditoria() {
    }

    public EventoAuditoria(String usuario, String tipo, String entidad,
                           String entidadId, String detalle) {
        this.usuario = usuario;
        this.tipo = tipo;
        this.entidad = entidad;
        this.entidadId = entidadId;
        this.detalle = detalle;
    }

    public Long getId() {
        return id;
    }

    public String getUsuario() {
        return usuario;
    }

    public String getTipo() {
        return tipo;
    }

    public String getEntidad() {
        return entidad;
    }

    public String getEntidadId() {
        return entidadId;
    }

    public String getDetalle() {
        return detalle;
    }

    public Instant getFecha() {
        return fecha;
    }
}