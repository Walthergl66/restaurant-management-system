package com.restaurante.usuarios.infrastructure.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * Emite y valida tokens JWT (HS256). Las autoridades viajan dentro del token
 * para no consultar la base de datos en cada petición.
 */
@Component
public class JwtService {

    static final String CLAIM_AUTHORITIES = "autoridades";
    static final String CLAIM_ROL = "rol";

    private final JwtProperties properties;
    private SecretKey key;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    void initKey() {
        String secret = properties.getSecret().trim();
        if (secret.length() < 32) {
            throw new IllegalStateException(
                    "JWT_SECRET debe tener al menos 32 caracteres (256 bits para HS256)");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generarAcceso(String username, String rol, Set<String> permisos) {
        return Jwts.builder()
                .subject(username)
                .claim(CLAIM_ROL, rol)
                .claim(CLAIM_AUTHORITIES, List.copyOf(permisos))
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plus(properties.getExpiration())))
                .signWith(key)
                .compact();
    }

    public String generarAccesoMas(Claims claims) {
        return Jwts.builder()
                .subject(claims.getSubject())
                .claim(CLAIM_ROL, claims.get(CLAIM_ROL, String.class))
                .claim(CLAIM_AUTHORITIES, claims.get(CLAIM_AUTHORITIES, List.class))
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plus(properties.getExpiration())))
                .signWith(key)
                .compact();
    }

    /**
     * @return claims si el token es válido (firma y vigencia), {@code null} en caso contrario
     */
    public Claims validar(String token) {
        try {
            return Jwts.parser().verifyWith(key).build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }

    public Duration getExpiration() {
        return properties.getExpiration();
    }
}