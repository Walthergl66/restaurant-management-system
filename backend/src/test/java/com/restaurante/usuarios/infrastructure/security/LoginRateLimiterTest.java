package com.restaurante.usuarios.infrastructure.security;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A-07: el rate limiter es atómico por clave, caduca por TTL y purga las
 *  entradas expiradas para acotar la memoria. */
class LoginRateLimiterTest {

    private final Instant inicio = Instant.parse("2026-09-23T00:00:00Z");

    private MutableClock reloj() {
        return new MutableClock(inicio);
    }

    @Test
    void bloqueaTrasCincoIntentosDentroDeLaVentana() {
        MutableClock clock = reloj();
        LoginRateLimiter limiter = new LoginRateLimiter(clock);
        String ip = "10.0.0.1";

        assertTrue(limiter.isAllowed(ip));
        assertTrue(limiter.isAllowed(ip));
        assertTrue(limiter.isAllowed(ip));
        assertTrue(limiter.isAllowed(ip));
        assertTrue(limiter.isAllowed(ip));
        assertFalse(limiter.isAllowed(ip), "el sexto intento debe bloquearse");
        assertFalse(limiter.isAllowed(ip));
    }

    @Test
    void reabreCuandoCaducaLaVentana() {
        MutableClock clock = reloj();
        LoginRateLimiter limiter = new LoginRateLimiter(clock);
        for (int i = 0; i < 5; i++) {
            assertTrue(limiter.isAllowed("10.0.0.2"));
        }
        assertFalse(limiter.isAllowed("10.0.0.2"));

        clock.avanzar(LoginRateLimiter.WINDOW.plusSeconds(1));
        assertTrue(limiter.isAllowed("10.0.0.2"), "tras la ventana el límite se reabre");
    }

    @Test
    void noConfundeClavesDistintas() {
        MutableClock clock = reloj();
        LoginRateLimiter limiter = new LoginRateLimiter(clock);
        for (int i = 0; i < 5; i++) {
            assertTrue(limiter.isAllowed("10.0.0.3"));
        }
        assertFalse(limiter.isAllowed("10.0.0.3"));
        assertTrue(limiter.isAllowed("10.0.0.4"));
    }

    @Test
    void purgaEntradasCaducadasParaAcotarMemoria() {
        MutableClock clock = reloj();
        LoginRateLimiter limiter = new LoginRateLimiter(clock);
        for (int i = 0; i < 5; i++) {
            assertTrue(limiter.isAllowed("ip-" + i));
        }
        assertEquals(5, limiter.clavesActivas());

        clock.avanzar(LoginRateLimiter.WINDOW.plusSeconds(1));
        limiter.purgarExpirados(clock.instant());
        assertEquals(0, limiter.clavesActivas(), "las claves expiradas se purgan");
    }

    static final class MutableClock extends Clock {
        private Instant ahora;

        MutableClock(Instant inicio) {
            this.ahora = inicio;
        }

        void avanzar(java.time.Duration d) {
            ahora = ahora.plus(d);
        }

        @Override
        public Instant instant() {
            return ahora;
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }
    }
}