package com.restaurante.pedidos.domain;

import com.restaurante.shared.domain.AuditableEntity;

/**
 * Ingrediente removido de una línea (p. ej. "sin cebolla").
 */
public class IngredienteRemovido extends AuditableEntity {

    private PedidoLinea linea;

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