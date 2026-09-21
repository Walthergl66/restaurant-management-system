package com.restaurante.caja.domain;

import com.restaurante.shared.domain.AuditableEntity;
import com.restaurante.shared.domain.Money;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Movimiento de una caja: INGRESO (cobros) o EGRESO (gastos manuales). Es
 * solo-inserción; los cierres se concilian contra la suma de movimientos.
 */
@Entity
@Table(name = "caja_movimientos")
public class MovimientoCaja extends AuditableEntity {

    @Column(name = "caja_id", nullable = false)
    private Long cajaId;

    @Column(nullable = false, length = 10)
    private String tipo;

    @Column(nullable = false, length = 120)
    private String concepto;

    @Column(nullable = false)
    private Money monto;

    @Column(length = 20)
    private String metodo;

    @Column(name = "pago_id")
    private Long pagoId;

    protected MovimientoCaja() {
    }

    public MovimientoCaja(Long cajaId, String tipo, String concepto, Money monto,
                          String metodo, Long pagoId) {
        this.cajaId = cajaId;
        this.tipo = tipo;
        this.concepto = concepto;
        this.monto = monto;
        this.metodo = metodo;
        this.pagoId = pagoId;
    }

    public Long getCajaId() {
        return cajaId;
    }

    public String getTipo() {
        return tipo;
    }

    public String getConcepto() {
        return concepto;
    }

    public Money getMonto() {
        return monto;
    }

    public String getMetodo() {
        return metodo;
    }

    public Long getPagoId() {
        return pagoId;
    }
}