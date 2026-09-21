package com.restaurante.pedidos.domain;

import com.restaurante.shared.domain.BaseEntity;
import com.restaurante.shared.domain.Money;
import com.restaurante.shared.infrastructure.MoneyConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Extra congelado en una línea de pedido (nombre y precio de catálogo).
 */
@Entity
@Table(name = "pedido_linea_extras")
public class ExtraLinea extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "linea_id", nullable = false)
    private PedidoLinea linea;

    @Column(name = "extra_id", nullable = false)
    private Long extraId;

    @Column(name = "nombre_extra", nullable = false, length = 80)
    private String nombre;

    @Convert(converter = MoneyConverter.class)
    @Column(nullable = false)
    private Money precio;

    protected ExtraLinea() {
    }

    public ExtraLinea(Long extraId, String nombre, Money precio) {
        this.extraId = extraId;
        this.nombre = nombre;
        this.precio = precio;
    }

    void agregarALinea(PedidoLinea linea) {
        this.linea = linea;
    }

    public PedidoLinea getLinea() {
        return linea;
    }

    public Long getExtraId() {
        return extraId;
    }

    public String getNombre() {
        return nombre;
    }

    public Money getPrecio() {
        return precio;
    }
}