package com.restaurante.cuentas.domain;

import com.restaurante.shared.domain.AuditableEntity;
import com.restaurante.shared.domain.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * Cuenta de una mesa. Agrupa los pedidos de un turno (RF-24): la agrupa por
 * mesa con una sola cuenta abierta a la vez. El total SIEMPRE se calcula desde
 * los registros (pedidos confirmados − anulaciones aprobadas, RNF-16) en
 * {@link CalculadoraCuenta}, nunca se guarda un acumulado.
 */
@Entity
@Table(name = "cuentas")
public class Cuenta extends AuditableEntity {

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(name = "mesa_id", nullable = false)
    private Long mesaId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoCuenta estado = EstadoCuenta.ABIERTA;

    protected Cuenta() {
    }

    public Cuenta(Long mesaId) {
        this.mesaId = mesaId;
    }

    /**
     * Cierra la cuenta: solo puede cerrarse una cuenta abierta.
     */
    public void cerrar() {
        if (estado != EstadoCuenta.ABIERTA) {
            throw new BusinessRuleException("La cuenta ya está cerrada");
        }
        this.estado = EstadoCuenta.CERRADA;
    }

    public Long getMesaId() {
        return mesaId;
    }

    public EstadoCuenta getEstado() {
        return estado;
    }

    public Long getVersion() {
        return version;
    }
}