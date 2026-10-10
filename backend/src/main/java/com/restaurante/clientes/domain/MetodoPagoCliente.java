package com.restaurante.clientes.domain;

import com.restaurante.shared.domain.BaseEntity;
import com.restaurante.shared.domain.exception.BusinessRuleException;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Set;

/**
 * Método de pago guardado por el cliente (RF-45). Solo guarda metadata no
 * sensible (tipo, alias, últimos 4 dígitos): nunca el número completo de la
 * tarjeta ni el CVV.
 */
@Entity
@Table(name = "metodos_pago_cliente", schema = "comercial")
public class MetodoPagoCliente extends BaseEntity {

    private static final Set<String> TIPOS = Set.of("EFECTIVO", "TARJETA", "TRANSFERENCIA", "OTRO");

    @Column(name = "cliente_id", nullable = false)
    private Long clienteId;

    @Column(nullable = false, length = 20)
    private String tipo;

    @Column(nullable = false, length = 40)
    private String alias;

    @Column(name = "ultimos4", length = 4)
    private String ultimos4;

    @Column(nullable = false)
    private Boolean predeterminado = false;

    @Column(nullable = false)
    private Boolean activo = true;

    @Column(name = "creado_at", nullable = false, updatable = false)
    private Instant creadoAt;

    @Column(name = "actualizado_at", nullable = false)
    private Instant actualizadoAt;

    protected MetodoPagoCliente() {
    }

    public MetodoPagoCliente(Long clienteId, String tipo, String alias, String ultimos4, Boolean predeterminado) {
        this.clienteId = clienteId;
        this.tipo = validarTipo(tipo);
        this.alias = alias;
        this.ultimos4 = ultimos4;
        this.predeterminado = predeterminado != null && predeterminado;
        this.activo = true;
    }

    private static String validarTipo(String tipo) {
        String up = tipo == null ? null : tipo.trim().toUpperCase();
        if (up == null || !TIPOS.contains(up)) {
            throw new BusinessRuleException("Tipo de método de pago no soportado: " + tipo);
        }
        return up;
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

    public void desactivar() {
        this.activo = false;
    }

    public void marcarPredeterminado() {
        this.predeterminado = true;
    }

    public Long getClienteId() { return clienteId; }
    public String getTipo() { return tipo; }
    public String getAlias() { return alias; }
    public String getUltimos4() { return ultimos4; }
    public Boolean getPredeterminado() { return predeterminado; }
    public Boolean getActivo() { return activo; }
    public Instant getCreadoAt() { return creadoAt; }
    public Instant getActualizadoAt() { return actualizadoAt; }
}
