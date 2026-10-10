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
@Table(name = "usuarios", schema = "seguridad")
public class Usuario extends AuditableEntity {

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false)
    private boolean activo = true;

    /**
     * Correo verificado (RF-45). Los usuarios creados por administración y los
     * preexistentes nacen verificados; el auto-registro de cliente nace en
     * falso hasta confirmar el código enviado a su correo.
     */
    @Column(name = "email_verificado", nullable = false)
    private boolean emailVerificado = true;

    @Column(name = "sesion_version", nullable = false)
    private long sesionVersion = 0;

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

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getNombre() {
        return nombre;
    }

    public boolean isActivo() {
        return activo;
    }

    public boolean isEmailVerificado() {
        return emailVerificado;
    }

    /** Marca la cuenta como pendiente de verificar el correo. */
    public void requiereVerificacionEmail() {
        this.emailVerificado = false;
    }

    public void marcarEmailVerificado() {
        this.emailVerificado = true;
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

    public long getSesionVersion() {
        return sesionVersion;
    }

    /**
     * Incrementa la versión de sesión. A-04: invalida los JWT ya emitidos
     * (el filtro compara esta versión con la embebida) tras un cambio de
     * credenciales/rol/estado; los refresh tokens activos se revocan aparte.
     */
    public void incrementarSesionVersion() {
        this.sesionVersion++;
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