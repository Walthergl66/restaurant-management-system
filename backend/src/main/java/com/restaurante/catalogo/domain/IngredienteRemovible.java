package com.restaurante.catalogo.domain;

import com.restaurante.shared.domain.AuditableEntity;
import com.restaurante.shared.domain.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Ingrediente que el cliente puede pedir remover de un producto
 * (sin cebolla, sin tomate...).
 */
@Entity
@Table(name = "producto_ingredientes")
public class IngredienteRemovible extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @Column(nullable = false, length = 80)
    private String nombre;

    @Column(nullable = false)
    private boolean activo = true;

    protected IngredienteRemovible() {
    }

    public IngredienteRemovible(Producto producto, String nombre) {
        this.producto = producto;
        cambiarNombre(nombre);
    }

    public Producto getProducto() {
        return producto;
    }

    public String getNombre() {
        return nombre;
    }

    public boolean isActivo() {
        return activo;
    }

    public void cambiarNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new BusinessRuleException("El nombre del ingrediente no puede estar vacío");
        }
        this.nombre = nombre.trim();
    }

    public void activar() {
        this.activo = true;
    }

    public void desactivar() {
        this.activo = false;
    }
}