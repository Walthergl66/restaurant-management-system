package com.restaurante.caja.domain;

import com.restaurante.shared.domain.AuditableEntity;
import com.restaurante.shared.domain.Money;
import com.restaurante.shared.domain.exception.BusinessRuleException;
import com.restaurante.shared.infrastructure.MoneyConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;

/**
 * Caja (turno) del establecimiento (RF-32 a RF-35). Toda transición de estado
 * vive aquí: la apertura debe darse sobre una caja nueva y el cierre sobre una
 * abierta. El cierre exige la conciliación y calcula la diferencia en una sola
 * clase. RNF-11: {@code @Version} optimista contra cierres simultáneos.
 */
@Entity
@Table(name = "cajas")
public class Caja extends AuditableEntity {

    @Version
    @Column(nullable = false)
    private Long version;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoCaja estado = EstadoCaja.ABIERTA;

    @Convert(converter = MoneyConverter.class)
    @Column(name = "apertura_inicial", nullable = false)
    private Money aperturaInicial = Money.ZERO;

    @Convert(converter = MoneyConverter.class)
    @Column(name = "cierre_esperado")
    private Money cierreEsperado;

    @Convert(converter = MoneyConverter.class)
    @Column(name = "cierre_real")
    private Money cierreReal;

    @Convert(converter = MoneyConverter.class)
    @Column
    private Money diferencia;

    @Column(name = "abierta_por", length = 50)
    private String abiertaPor;

    @Column(name = "cerrada_por", length = 50)
    private String cerradaPor;

    @Column(name = "abierta_at", nullable = false)
    private Instant abiertaAt;

    @Column(name = "cerrada_at")
    private Instant cerradaAt;

    protected Caja() {
    }

    public Caja(Money aperturaInicial, String abiertaPor) {
        this.aperturaInicial = aperturaInicial;
        this.abiertaPor = abiertaPor;
        this.abiertaAt = Instant.now();
    }

    public void cerrar(Money cierreReal, Money cierreEsperado, String cerradaPor) {
        if (estado != EstadoCaja.ABIERTA) {
            throw new BusinessRuleException("La caja ya está cerrada");
        }
        this.cierreEsperado = cierreEsperado;
        this.cierreReal = cierreReal;
        this.diferencia = cierreReal.subtract(cierreEsperado);
        this.cerradaPor = cerradaPor;
        this.cerradaAt = Instant.now();
        this.estado = EstadoCaja.CERRADA;
    }

    public Long getVersion() {
        return version;
    }

    public EstadoCaja getEstado() {
        return estado;
    }

    public Money getAperturaInicial() {
        return aperturaInicial;
    }

    public Money getCierreEsperado() {
        return cierreEsperado;
    }

    public Money getCierreReal() {
        return cierreReal;
    }

    public Money getDiferencia() {
        return diferencia;
    }

    public String getAbiertaPor() {
        return abiertaPor;
    }

    public String getCerradaPor() {
        return cerradaPor;
    }

    public Instant getAbiertaAt() {
        return abiertaAt;
    }

    public Instant getCerradaAt() {
        return cerradaAt;
    }
}