package com.restaurante.catalogo.domain;

import com.restaurante.shared.domain.AuditableEntity;
import com.restaurante.shared.domain.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Categoría del menú (entradas, platos fuertes, postres...).
 */
@Entity
@Table(name = "categorias")
public class Categoria extends AuditableEntity {

    @Column(nullable = false, unique = true, length = 80)
    private String nombre;

    @Column(length = 200)
    private String descripcion;

    @Column(nullable = false)
    private int orden;

    @Column(nullable = false)
    private boolean activo = true;

    protected Categoria() {
    }

    public Categoria(String nombre, int orden) {
        cambiarNombre(nombre);
        this.orden = orden;
    }

    public String getNombre() {
        return nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public int getOrden() {
        return orden;
    }

    public boolean isActivo() {
        return activo;
    }

    public void cambiarNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new BusinessRuleException("El nombre de la categoría no puede estar vacío");
        }
        this.nombre = nombre.trim();
    }

    public void cambiarDescripcion(String descripcion) {
        this.descripcion = descripcion == null || descripcion.isBlank() ? null : descripcion.trim();
    }

    public void cambiarOrden(int orden) {
        this.orden = orden;
    }

    public void activar() {
        this.activo = true;
    }

    public void desactivar() {
        this.activo = false;
    }
}