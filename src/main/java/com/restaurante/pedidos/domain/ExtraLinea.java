package com.restaurante.pedidos.domain;

import com.restaurante.shared.domain.AuditableEntity;
import com.restaurante.shared.domain.Money;

/**
 * Extra congelado en una línea de pedido (nombre y precio de catálogo).
 */
public class ExtraLinea extends AuditableEntity {

    private PedidoLinea linea;

    private Long extraId;

    private String nombre;

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