package com.restaurante.usuarios.infrastructure.security;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limiter de intentos de login/refresh — RNF-07: máximo 5 por IP por
 * minuto. A-07: el registro de intentos es atómico por clave (compute sobre
 * {@link ConcurrentHashMap}), los intentos caducan por TTL y las claves
 * expiradas se purgan para acotar la memoria frente a IPs distintas.
 */
@Component
public class LoginRateLimiter {

    static final int MAX_ATTEMPTS = 5;
    static final Duration WINDOW = Duration.ofMinutes(1);
    static final int MAX_KEYS = 10_000;

    private final Clock clock;
    private final ConcurrentHashMap<String, ArrayDeque<Instant>> attempts = new ConcurrentHashMap<>();

    public LoginRateLimiter() {
        this(Clock.systemUTC());
    }

    LoginRateLimiter(Clock clock) {
        this.clock = clock;
    }

    /**
     * @return {@code true} si la clave aún tiene intentos disponibles. El
     *         registro es atómico por clave: bajo concurrencia nunca se supera
     *         el máximo de la ventana.
     */
    public boolean isAllowed(String key) {
        boolean[] permitido = {false};
        Instant now = clock.instant();
        attempts.compute(key, (k, ventana) -> {
            ArrayDeque<Instant> deque = ventana == null ? new ArrayDeque<>() : ventana;
            Instant corte = now.minus(WINDOW);
            while (!deque.isEmpty() && deque.peekFirst().isBefore(corte)) {
                deque.pollFirst();
            }
            if (deque.size() >= MAX_ATTEMPTS) {
                permitido[0] = false;
                return deque;
            }
            deque.addLast(now);
            permitido[0] = true;
            return deque;
        });
        if (attempts.size() > MAX_KEYS) {
            purgarExpirados(now);
        }
        return permitido[0];
    }

    /** Elimina las entradas cuya ventana ya caducó: acota la memoria aunque
     *  lleguen IPs distintas (A-07). Las claves dentro de la ventana se
     *  conservan para no invalidar el contador. */
    void purgarExpirados(Instant now) {
        Instant corte = now.minus(WINDOW);
        attempts.entrySet().removeIf(e -> e.getValue().isEmpty() || e.getValue().peekLast().isBefore(corte));
    }

    public void reset(String key) {
        attempts.remove(key);
    }

    int clavesActivas() {
        return attempts.size();
    }
}