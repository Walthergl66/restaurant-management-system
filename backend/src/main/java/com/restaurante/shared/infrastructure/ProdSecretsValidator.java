package com.restaurante.shared.infrastructure;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Fallo rápido en el arranque del perfil {@code prod}: verifica que las
 * variables obligatorias existan y no lleguen vacías, y que el secreto JWT
 * cumpla la longitud mínima. Evita que la app arranque con contraseñas o
 * claves por defecto o con placeholders sin resolver (p.ej. la forma
 * {@code ${VAR:?mensaje}} NO corta el arranque en Spring: resolvería al
 * literal {@code ?mensaje}). Se activa solo con el perfil {@code prod}.
 */
@Component
@Profile("prod")
public class ProdSecretsValidator {

    /** Mínimo que exige JwtService para HS256. */
    static final int JWT_SECRET_MIN_LEN = 32;

    private final Environment environment;

    public ProdSecretsValidator(Environment environment) {
        this.environment = environment;
    }

    @PostConstruct
    void validar() {
        List<String> faltantes = new ArrayList<>();
        for (String var : List.of("DB_USER", "DB_PASSWORD", "ADMIN_INITIAL_PASSWORD",
                "JWT_SECRET", "CORS_ALLOWED_ORIGINS")) {
            String valor = environment.getProperty(var);
            if (valor == null || valor.isBlank()) {
                faltantes.add(var);
            }
        }
        if (!faltantes.isEmpty()) {
            throw new IllegalStateException("Faltan variables obligatorias de producción: "
                    + String.join(", ", faltantes)
                    + ". Suminístrelas por entorno (nunca con valores por defecto).");
        }
        String jwtSecret = environment.getProperty("JWT_SECRET");
        if (jwtSecret != null && jwtSecret.trim().length() < JWT_SECRET_MIN_LEN) {
            throw new IllegalStateException("JWT_SECRET debe tener al menos "
                    + JWT_SECRET_MIN_LEN + " caracteres (256 bits para HS256)");
        }
    }
}