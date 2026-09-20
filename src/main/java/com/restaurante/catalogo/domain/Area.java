package com.restaurante.catalogo.domain;

import com.restaurante.shared.domain.AuditableEntity;
import com.restaurante.shared.domain.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Área de preparación: divide las comandas (cocina, barra, postres...).
 */
@Entity
@Table(name = "areas")
public class Area extends AuditableEntity {

    @Column(nullable = false, unique = true, length = 80)
    private String nombre;

    @Column(length = 200)
    private String descripcion;

    @Column(nullable = false)
    private boolean activo = true;

    protected Area() {
    }

    public Area(String nombre) {
        cambiarNombre(nombre);
    }

    public String getNombre() {
        return nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public boolean isActivo() {
        return activo;
    }

    public void cambiarNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new BusinessRuleException("El nombre del área no puede estar vacío");
        }
        this.nombre = nombre.trim();
    }

    public void cambiarDescripcion(String descripcion) {
        this.descripcion = descripcion == null || descripcion.isBlank() ? null : descripcion.trim();
    }

    public void activar() {
        this.activo = true;
    }

    public void desactivar() {
        this.activo = false;
    }
}