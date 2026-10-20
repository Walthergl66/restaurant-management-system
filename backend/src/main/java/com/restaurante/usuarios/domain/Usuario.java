package com.restaurante.usuarios.domain;

import com.restaurante.shared.domain.AuditableEntity;
import com.restaurante.shared.domain.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.Collections;
import java.util.LinkedHashSet;
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

    /**
     * Permisos explícitos asignados por administración (usuarios_permisos).
     * Cuando tiene filas, mandan sobre los del rol: el administrador puede
     * agregar o quitar funcionalidades por usuario desde el panel. Vacío
     * (por defecto) = rigen los permisos por defecto del rol.
     */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "usuarios_permisos", schema = "seguridad",
            joinColumns = @JoinColumn(name = "usuario_id"),
            inverseJoinColumns = @JoinColumn(name = "permiso_id"))
    private Set<Permiso> permisosExplicitos = new LinkedHashSet<>();

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
     * Permisos efectivos: si el administrador asignó permisos explícitos al
     * usuario, esos mandan (agregar/quitar funcionalidades por usuario);
     * si no, rigen los permisos por defecto del rol.
     */
    public Set<String> getPermisoCodigos() {
        if (!activo) {
            return Collections.emptySet();
        }
        if (!permisosExplicitos.isEmpty()) {
            return permisosExplicitos.stream().map(Permiso::getCodigo)
                    .collect(java.util.stream.Collectors.toUnmodifiableSet());
        }
        return rol.getPermisoCodigos();
    }

    /** Permisos explícitos asignados por administración (vacío = defaults del rol). */
    public Set<String> getPermisosExplicitosCodigos() {
        return permisosExplicitos.stream().map(Permiso::getCodigo)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    /** Reemplaza el set explícito de permisos del usuario. */
    public void asignarPermisosExplicitos(Set<Permiso> permisos) {
        this.permisosExplicitos.clear();
        if (permisos != null) {
            this.permisosExplicitos.addAll(permisos);
        }
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

    /**
     * Cambia el correo (username) y marca la cuenta como pendiente de
     * verificar: el cliente debe confirmar el código enviado al nuevo correo
     * antes de poder iniciar sesión (RF-45).
     */
    public void cambiarCorreo(String nuevoCorreo) {
        if (nuevoCorreo == null || nuevoCorreo.isBlank()) {
            throw new BusinessRuleException("El correo no puede estar vacío");
        }
        this.username = nuevoCorreo.trim().toLowerCase();
        this.emailVerificado = false;
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