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

    // --- Correo transaccional (RF-45): validación condicional ---------------

    @Test
    void mailApagadoNoExigeCredenciales() {
        String[] apagado = concat(COMPLETO, "MAIL_ENABLED", "false");
        assertDoesNotThrow(con(apagado)::validar);
    }

    @Test
    void mailEncendidoSinCredencialesLanza() {
        String[] encendido = concat(COMPLETO, "MAIL_ENABLED", "true");
        IllegalStateException e = assertThrows(IllegalStateException.class,
                con(encendido)::validar);
        assertTrue(e.getMessage().contains("MAIL_ENABLED"));
        assertTrue(e.getMessage().contains("RESEND_API_KEY"));
        assertTrue(e.getMessage().contains("MAIL_FROM"));
    }

    @Test
    void mailEncendidoConCredencialesNoLanza() {
        String[] completo = concat(COMPLETO,
                "MAIL_ENABLED", "true",
                "RESEND_API_KEY", "re_test",
                "MAIL_FROM", "onboarding@resend.dev",
                "MAIL_FROM_NAME", "Restaurante");
        assertDoesNotThrow(con(completo)::validar);
    }

    @Test
    void mailEncendidoConCredencialVaciaCuentaComoFaltante() {
        String[] incompleto = concat(COMPLETO,
                "MAIL_ENABLED", "true",
                "RESEND_API_KEY", " ",
                "MAIL_FROM", "onboarding@resend.dev",
                "MAIL_FROM_NAME", "Restaurante");
        IllegalStateException e = assertThrows(IllegalStateException.class,
                con(incompleto)::validar);
        assertTrue(e.getMessage().contains("RESEND_API_KEY"));
    }

    private static String[] concat(String[] base, String... extras) {
        String[] resultado = new String[base.length + extras.length];
        System.arraycopy(base, 0, resultado, 0, base.length);
        System.arraycopy(extras, 0, resultado, base.length, extras.length);
        return resultado;
    }
}