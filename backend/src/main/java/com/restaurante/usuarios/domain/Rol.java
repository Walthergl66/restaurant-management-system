package com.restaurante.usuarios.domain;

import com.restaurante.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Rol de acceso. Agrupa permisos que se conceden a los usuarios.
 *
 * <p>Cada rol pertenece a una {@link CategoriaRol} (ADMIN, EMPLEADO,
 * CLIENTE o DELIVERY) y lleva dos banderas que gobiernan qué puede hacer el
 * administrador con él:
 * <ul>
 *   <li>{@code asignablePorAdmin}: si es {@code false}, el panel no puede
 *       crear ni asignar ese rol (ADMIN: hay una sola cuenta; CLIENTE y
 *       DELIVERY: nacen por auto-registro).</li>
 *   <li>{@code configurable}: si es {@code true}, el administrador puede
 *       agregar o quitar funcionalidades (permisos explícitos) a los
 *       usuarios con ese rol.</li>
 * </ul>
 */
@Entity
@Table(name = "roles", schema = "seguridad")
public class Rol extends BaseEntity {

    @Column(nullable = false, unique = true, length = 30)
    private String codigo;

    @Column(length = 120)
    private String descripcion;

    @Column(nullable = false)
    private boolean activo = true;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "categoria_id", nullable = false)
    private CategoriaRol categoria;

    @Column(name = "asignable_por_admin", nullable = false)
    private boolean asignablePorAdmin = true;

    @Column(nullable = false)
    private boolean configurable = false;

    @Column(nullable = false, length = 20)
    private String origen = "ADMIN";

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "roles_permisos", schema = "seguridad",
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

    public CategoriaRol getCategoria() {
        return categoria;
    }

    /** Si el administrador puede crear usuarios con este rol desde el panel. */
    public boolean isAsignablePorAdmin() {
        return asignablePorAdmin;
    }

    /** Si el administrador puede ajustar las funcionalidades de este rol. */
    public boolean isConfigurable() {
        return configurable;
    }

    /** De dónde nace la cuenta con este rol: {@code ADMIN} o {@code AUTO_REGISTRO}. */
    public String getOrigen() {
        return origen;
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