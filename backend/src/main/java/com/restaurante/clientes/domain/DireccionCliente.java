package com.restaurante.clientes.domain;

import com.restaurante.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "clientes_direcciones")
public class DireccionCliente extends BaseEntity {

    @Column(name = "cliente_id", nullable = false)
    private Long clienteId;

    @Column(nullable = false, length = 40)
    private String etiqueta;

    @Column(nullable = false, length = 200)
    private String direccion;

    @Column(length = 20)
    private String telefono;

    @Column(length = 200)
    private String observaciones;

    @Column(nullable = false)
    private Boolean activa = true;

    @Column(name = "creado_at", nullable = false, updatable = false)
    private Instant creadoAt;

    @Column(name = "actualizado_at", nullable = false)
    private Instant actualizadoAt;

    protected DireccionCliente() {
    }

    public DireccionCliente(Long clienteId, String etiqueta, String direccion, String telefono, String observaciones) {
        this.clienteId = clienteId;
        this.etiqueta = etiqueta;
        this.direccion = direccion;
        this.telefono = telefono;
        this.observaciones = observaciones;
        this.activa = true;
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

    public Long getClienteId() { return clienteId; }
    public String getEtiqueta() { return etiqueta; }
    public String getDireccion() { return direccion; }
    public String getTelefono() { return telefono; }
    public String getObservaciones() { return observaciones; }
    public Boolean getActiva() { return activa; }
    public Instant getCreadoAt() { return creadoAt; }
    public Instant getActualizadoAt() { return actualizadoAt; }
}
