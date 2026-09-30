package com.restaurante.usuarios.infrastructure.security;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

/**
 * Configuración del JWT. El secreto llega por entorno ({@code JWT_SECRET}) y
 * debe tener al menos {@value #JWT_SECRET_MIN_LEN} caracteres (256 bits) para
 * HS256. No hay valor por defecto en ningún perfil: si falta, el arranque falla
 * aquí en vez de firmar tokens con una clave conocida o vacía.
 */
@Validated
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {

    /** Longitud mínima del secreto; la regla vive aquí y la reutiliza JwtService. */
    public static final int JWT_SECRET_MIN_LEN = 32;

    @NotBlank(message = "JWT_SECRET es obligatorio: sin valor por defecto en ningún perfil")
    @Size(min = JWT_SECRET_MIN_LEN,
            message = "JWT_SECRET debe tener al menos 32 caracteres (256 bits para HS256)")
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