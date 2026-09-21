package com.restaurante.pedidos.domain;

import com.restaurante.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Ingrediente removido de una línea (p. ej. "sin cebolla").
 */
@Entity
@Table(name = "pedido_linea_ingredientes")
public class IngredienteRemovido extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "linea_id", nullable = false)
    private PedidoLinea linea;

    @Column(nullable = false, length = 80)
    private String nombre;

    protected IngredienteRemovido() {
    }

    public IngredienteRemovido(String nombre) {
        this.nombre = nombre;
    }

    void agregarALinea(PedidoLinea linea) {
        this.linea = linea;
    }

    public String getNombre() {
        return nombre;
    }
}