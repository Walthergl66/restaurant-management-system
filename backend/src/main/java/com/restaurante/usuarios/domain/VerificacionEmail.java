package com.restaurante.usuarios.domain;

import com.restaurante.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Código de verificación de correo del cliente (RF-45): un solo uso, con
 * expiración y guardado hasheado (SHA-256), igual que el token de recuperación
 * de contraseña. Nunca se guarda el código en claro.
 */
@Entity
@Table(name = "verificacion_email", schema = "seguridad")
public class VerificacionEmail extends BaseEntity {

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "codigo_hash", nullable = false, length = 64)
    private String codigoHash;

    @Column(name = "expira_at", nullable = false)
    private Instant expiraAt;

    @Column(nullable = false)
    private boolean usado = false;

    @Column(name = "creado_at", nullable = false, updatable = false)
    private Instant creadoAt;

    protected VerificacionEmail() {
    }

    public VerificacionEmail(Long usuarioId, String codigoHash, Instant expiraAt) {
        this.usuarioId = usuarioId;
        this.codigoHash = codigoHash;
        this.expiraAt = expiraAt;
    }

    @PrePersist
    void prePersist() {
        if (creadoAt == null) {
            creadoAt = Instant.now();
        }
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public boolean isUsado() {
        return usado;
    }

    public boolean isExpirado() {
        return expiraAt.isBefore(Instant.now());
    }

    public void marcarUsado() {
        this.usado = true;
    }

    /** Comprueba si el código crudo corresponde al hash guardado. */
    public boolean coincide(String codigo) {
        return codigo != null && codigoHash.equals(hash(codigo));
    }

    /** Hashea el código crudo; es el valor que se guarda y se compara. */
    public static String hash(String codigo) {
        return RefreshToken.sha256(codigo);
    }
}
