package com.restaurante.usuarios.domain;

import com.restaurante.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Token de recuperación de contraseña (RF-45): un solo uso, con expiración y
 * guardado hasheado (SHA-256) igual que el token de refresco.
 */
@Entity
@Table(name = "recuperacion_password", schema = "seguridad")
public class RecuperacionPassword extends BaseEntity {

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "expira_at", nullable = false)
    private Instant expiraAt;

    @Column(nullable = false)
    private boolean usado = false;

    @Column(name = "creado_at", nullable = false, updatable = false)
    private Instant creadoAt;

    protected RecuperacionPassword() {
    }

    public RecuperacionPassword(Long usuarioId, String tokenHash, Instant expiraAt) {
        this.usuarioId = usuarioId;
        this.tokenHash = tokenHash;
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

    /** Hashea el token crudo; es el valor que se guarda y se compara. */
    public static String hash(String token) {
        return RefreshToken.sha256(token);
    }
}
