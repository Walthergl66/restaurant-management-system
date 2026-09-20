package com.restaurante.usuarios.domain;

import com.restaurante.shared.domain.AuditableEntity;
import com.restaurante.shared.domain.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.Collections;
import java.util.Set;

/**
 * Usuario del sistema. Entidad mutable hasta ser confiable; su contraseña se
 * guarda siempre con un hash seguro (BCrypt).
 */
@Entity
@Table(name = "usuarios")
public class Usuario extends AuditableEntity {

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false)
    private boolean activo = true;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "rol_id", nullable = false)
    private Rol rol;

    protected Usuario() {
    }

    public Usuario(String username, String passwordHash, String nombre, Rol rol) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.nombre = nombre;
        this.rol = rol;
    }

    public String getUsername() {
        return username;
    }

    public String getNombre() {
        return nombre;
    }

    public boolean isActivo() {
        return activo;
    }

    public Rol getRol() {
        return rol;
    }

    /**
     * Permisos efectivos: los del rol si está activo.
     */
    public Set<String> getPermisoCodigos() {
        if (!activo) {
            return Collections.emptySet();
        }
        return rol.getPermisoCodigos();
    }

    public void cambiarNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new BusinessRuleException("El nombre no puede estar vacío");
        }
        this.nombre = nombre.trim();
    }

    public void cambiarPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public void asignarRol(Rol rol) {
        if (rol == null || !rol.isActivo()) {
            throw new BusinessRuleException("El rol asignado debe estar activo");
        }
        this.rol = rol;
    }

    public void activar() {
        this.activo = true;
    }

    /**
     * Desactivación lógica: no se borra al usuario para conservar la auditoría.
     */
    public void desactivar() {
        this.activo = false;
    }
}