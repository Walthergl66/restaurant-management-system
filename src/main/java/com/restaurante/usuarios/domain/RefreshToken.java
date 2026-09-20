package com.restaurante.usuarios.domain;

import com.restaurante.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Token de refresco emitido junto al JWT. Se guarda hasheado (SHA-256) para que
 * un robo de la base no sirva directamente. Admite revocación.
 */
@Entity
@Table(name = "refresh_tokens")
public class RefreshToken extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private boolean revoked = false;

    protected RefreshToken() {
    }

    public RefreshToken(Usuario usuario, String tokenHash, Instant expiresAt) {
        this.usuario = usuario;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public boolean isRevoked() {
        return revoked;
    }

    public boolean isExpired() {
        return expiresAt.isBefore(Instant.now());
    }

    /**
     * Hashea el token crudo; es el valor guardado y el que se compara.
     */
    public static String hash(UUID token) {
        return sha256(token.toString());
    }

    public static String sha256(String value) {
        try {
            var digest = java.security.MessageDigest.getInstance("SHA-256");
            var bytes = digest.digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(bytes);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }

    public void revocar() {
        this.revoked = true;
    }
}