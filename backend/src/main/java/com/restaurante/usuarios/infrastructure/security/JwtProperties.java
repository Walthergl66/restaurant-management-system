package com.restaurante.usuarios.infrastructure.security;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

/**
 * Configuración del JWT. El secreto llega por entorno ({@code JWT_SECRET}) y
 * debe tener al menos 32 bytes (256 bits) para HS256.
 */
@Validated
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {

    @NotNull
    private String secret = "";

    @Min(0)
    private long expirationMs = 86_400_000L;       // 24 h

    @Min(0)
    private long refreshExpirationMs = 604_800_000L; // 7 días

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public long getExpirationMs() {
        return expirationMs;
    }

    public void setExpirationMs(long expirationMs) {
        this.expirationMs = expirationMs;
    }

    public long getRefreshExpirationMs() {
        return refreshExpirationMs;
    }

    public void setRefreshExpirationMs(long refreshExpirationMs) {
        this.refreshExpirationMs = refreshExpirationMs;
    }

    public Duration getExpiration() {
        return Duration.of(expirationMs, ChronoUnit.MILLIS);
    }

    public Duration getRefreshExpiration() {
        return Duration.of(refreshExpirationMs, ChronoUnit.MILLIS);
    }
}