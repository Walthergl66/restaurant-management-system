package com.restaurante.shared.infrastructure;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A-?/plan: el validador de prod falla rápido si falta un secreto obligatorio
 *  o el JWT_SECRET no cumple la longitud mínima. */
class ProdSecretsValidatorTest {

    private ProdSecretsValidator con(String... pares) {
        MockEnvironment env = new MockEnvironment();
        for (int i = 0; i + 1 < pares.length; i += 2) {
            env.setProperty(pares[i], pares[i + 1]);
        }
        return new ProdSecretsValidator(env);
    }

    private static final String[] COMPLETO = {
            "DB_USER", "restaurante",
            "DB_PASSWORD", "s3cr3t0",
            "ADMIN_INITIAL_PASSWORD", "ClaveFuerte123!",
            "JWT_SECRET", "x".repeat(64),
            "CORS_ALLOWED_ORIGINS", "https://app.example.com"
    };

    @Test
    void conTodoPresenteNoLanza() {
        assertDoesNotThrow(con(COMPLETO)::validar);
    }

    @Test
    void faltaSecretLanzaConDichaVariable() {
        String[] sinSecret = {
                "DB_USER", "restaurante",
                "DB_PASSWORD", "s3cr3t0",
                "ADMIN_INITIAL_PASSWORD", "ClaveFuerte123!",
                "CORS_ALLOWED_ORIGINS", "https://app.example.com"
        };
        IllegalStateException e = assertThrows(IllegalStateException.class,
                con(sinSecret)::validar);
        assertTrue(e.getMessage().contains("JWT_SECRET"));
    }

    @Test
    void valorVacioCuentaComoFaltante() {
        IllegalStateException e = assertThrows(IllegalStateException.class,
                con("DB_USER", "restaurante", "DB_PASSWORD", "",
                        "ADMIN_INITIAL_PASSWORD", "x", "JWT_SECRET", "y".repeat(64),
                        "CORS_ALLOWED_ORIGINS", "https://app.example.com")::validar);
        assertTrue(e.getMessage().contains("DB_PASSWORD"));
    }

    @Test
    void jwtSecretCortoLanza() {
        String[] corto = {
                "DB_USER", "restaurante",
                "DB_PASSWORD", "s3cr3t0",
                "ADMIN_INITIAL_PASSWORD", "ClaveFuerte123!",
                "JWT_SECRET", "corta",
                "CORS_ALLOWED_ORIGINS", "https://app.example.com"
        };
        IllegalStateException e = assertThrows(IllegalStateException.class, con(corto)::validar);
        assertTrue(e.getMessage().contains("JWT_SECRET"));
    }
}