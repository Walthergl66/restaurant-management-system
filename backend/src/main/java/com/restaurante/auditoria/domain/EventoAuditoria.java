package com.restaurante.auditoria.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

/**
 * Evento de auditoría. Mapa a {@code auditoria_eventos}, una tabla de SOLO
 * INSERCIÓN (RNF-10): no hay actualización ni borrado desde la aplicación.
 */
@Entity
@Table(name = "auditoria_eventos", schema = "auditoria")
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
    @JdbcTypeCode(SqlTypes.JSON)
    private String detalle;

    @Column(nullable = false)
    private Instant fecha = Instant.now();

    @Column(nullable = false, length = 20)
    private String categoria;

    @JdbcTypeCode(SqlTypes.INET)
    @Column(columnDefinition = "inet")
    private String ip;

    @Column(name = "user_agent", length = 255)
    private String userAgent;

    @Column(name = "request_id", length = 36)
    private String requestId;

    protected EventoAuditoria() {
    }

    public EventoAuditoria(String usuario, String tipo, String entidad,
                           String entidadId, String detalle) {
        this(usuario, tipo, entidad, entidadId, detalle, "NEGOCIO", null, null, null);
    }

    public EventoAuditoria(String usuario, String tipo, String entidad,
                           String entidadId, String detalle,
                           String categoria, String ip, String userAgent, String requestId) {
        this.usuario = usuario;
        this.tipo = tipo;
        this.entidad = entidad;
        this.entidadId = entidadId;
        this.detalle = detalle;
        this.categoria = categoria == null ? "NEGOCIO" : categoria;
        this.ip = ip;
        this.userAgent = userAgent;
        this.requestId = requestId;
        if (this.fecha == null) {
            this.fecha = Instant.now();
        }
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

    public String getCategoria() {
        return categoria;
    }

    public String getIp() {
        return ip;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public String getRequestId() {
        return requestId;
    }
}