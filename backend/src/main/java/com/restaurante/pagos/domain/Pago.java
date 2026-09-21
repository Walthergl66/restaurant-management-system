package com.restaurante.pagos.domain;

import com.restaurante.shared.domain.AuditableEntity;
import com.restaurante.shared.domain.Money;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/**
 * Pago de una cuenta por un método determinado. Un cobro puede ser mixto:
 * varios pagos, uno por método, cuya suma cubre el total de la cuenta.
 */
@Entity
@Table(name = "pagos")
public class Pago extends AuditableEntity {

    @Column(name = "cuenta_id", nullable = false)
    private Long cuentaId;

    @Column(name = "caja_id", nullable = false)
    private Long cajaId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MetodoPago metodo;

    @Column(nullable = false)
    private Money monto;

    @Column(name = "cobrado_por", length = 50)
    private String cobradoPor;

    protected Pago() {
    }

    public Pago(Long cuentaId, Long cajaId, MetodoPago metodo, Money monto, String cobradoPor) {
        this.cuentaId = cuentaId;
        this.cajaId = cajaId;
        this.metodo = metodo;
        this.monto = monto;
        this.cobradoPor = cobradoPor;
    }

    public Long getCuentaId() {
        return cuentaId;
    }

    public Long getCajaId() {
        return cajaId;
    }

    public MetodoPago getMetodo() {
        return metodo;
    }

    public Money getMonto() {
        return monto;
    }

    public String getCobradoPor() {
        return cobradoPor;
    }
}