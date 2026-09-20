package com.restaurante.usuarios.domain;

import com.restaurante.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Permiso atómico representado por un código único
 * (ej. {@code pedidos:confirmar}).
 */
@Entity
@Table(name = "permisos")
public class Permiso extends BaseEntity {

    @Column(nullable = false, unique = true, length = 60)
    private String codigo;

    @Column(nullable = false, length = 30)
    private String modulo;

    @Column(length = 120)
    private String descripcion;

    @Column(nullable = false)
    private boolean activo = true;

    protected Permiso() {
    }

    public Permiso(String codigo, String modulo, String descripcion) {
        this.codigo = codigo;
        this.modulo = modulo;
        this.descripcion = descripcion;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getModulo() {
        return modulo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public boolean isActivo() {
        return activo;
    }
}