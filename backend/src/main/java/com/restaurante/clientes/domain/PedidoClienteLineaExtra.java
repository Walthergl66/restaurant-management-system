package com.restaurante.clientes.domain;

import com.restaurante.shared.domain.BaseEntity;
import com.restaurante.shared.domain.Money;
import com.restaurante.shared.infrastructure.MoneyConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "pedidos_clientes_lineas_extras")
public class PedidoClienteLineaExtra extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "linea_id", nullable = false)
    private PedidoClienteLinea linea;

    @Column(name = "extra_id", nullable = false)
    private Long extraId;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Convert(converter = MoneyConverter.class)
    @Column(nullable = false)
    private Money precio;

    @Column(name = "creado_at", nullable = false, updatable = false)
    private Instant creadoAt;

    protected PedidoClienteLineaExtra() {
    }

    public PedidoClienteLineaExtra(Long extraId, String nombre, Money precio) {
        this.extraId = extraId;
        this.nombre = nombre;
        this.precio = precio;
    }

    @PrePersist
    void prePersist() {
        if (creadoAt == null) creadoAt = Instant.now();
    }

    public void setLinea(PedidoClienteLinea linea) { this.linea = linea; }
    public Long getExtraId() { return extraId; }
    public String getNombre() { return nombre; }
    public Money getPrecio() { return precio; }
    public Instant getCreadoAt() { return creadoAt; }
}
