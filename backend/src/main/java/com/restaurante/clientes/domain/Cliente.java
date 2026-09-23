package com.restaurante.clientes.domain;

import com.restaurante.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;

/**
 * Perfil del cliente ligado a un usuario (RF-45). Tabla V9 clientes.
 */
@Entity
@Table(name = "clientes")
public class Cliente extends BaseEntity {

    @Column(name = "usuario_id", nullable = false, unique = true)
    private Long usuarioId;

    @Column(nullable = false, unique = true, length = 20)
    private String cedula;

    @Column(nullable = false, unique = true, length = 20)
    private String telefono;

    @Column(nullable = false, length = 80)
    private String nombre;

    @Version
    @Column(nullable = false)
    private Long version = 0L;

    @Column(name = "creado_at", nullable = false, updatable = false)
    private Instant creadoAt;

    @Column(name = "actualizado_at", nullable = false)
    private Instant actualizadoAt;

    protected Cliente() {
    }

    public Cliente(Long usuarioId, String cedula, String telefono, String nombre) {
        this.usuarioId = usuarioId;
        this.cedula = cedula;
        this.telefono = telefono;
        this.nombre = nombre;
    }

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (creadoAt == null) creadoAt = now;
        if (actualizadoAt == null) actualizadoAt = now;
    }

    @PreUpdate
    void preUpdate() {
        actualizadoAt = Instant.now();
    }

    public Long getUsuarioId() { return usuarioId; }
    public String getCedula() { return cedula; }
    public String getTelefono() { return telefono; }
    public String getNombre() { return nombre; }
    public Long getVersion() { return version; }
    public Instant getCreadoAt() { return creadoAt; }
    public Instant getActualizadoAt() { return actualizadoAt; }
}
