package com.restaurante.usuarios.domain;

import com.restaurante.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Rol de acceso. Agrupa permisos que se conceden a los usuarios.
 */
@Entity
@Table(name = "roles")
public class Rol extends BaseEntity {

    @Column(nullable = false, unique = true, length = 30)
    private String codigo;

    @Column(length = 120)
    private String descripcion;

    @Column(nullable = false)
    private boolean activo = true;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "roles_permisos",
            joinColumns = @JoinColumn(name = "rol_id"),
            inverseJoinColumns = @JoinColumn(name = "permiso_id"))
    private Set<Permiso> permisos = new LinkedHashSet<>();

    protected Rol() {
    }

    public Rol(String codigo, String descripcion) {
        this.codigo = codigo;
        this.descripcion = descripcion;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public boolean isActivo() {
        return activo;
    }

    public Set<Permiso> getPermisos() {
        return Set.copyOf(permisos);
    }

    /**
     * Códigos de permiso concedidos por este rol.
     */
    public Set<String> getPermisoCodigos() {
        return permisos.stream().map(Permiso::getCodigo).collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    public void agregarPermiso(Permiso permiso) {
        this.permisos.add(permiso);
    }

    public void quitarPermiso(Permiso permiso) {
        this.permisos.remove(permiso);
    }
}