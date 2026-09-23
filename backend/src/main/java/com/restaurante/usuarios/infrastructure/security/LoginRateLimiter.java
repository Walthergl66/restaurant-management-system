package com.restaurante.usuarios.infrastructure.security;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Rate limiter simple para login — RNF-07: máximo 5 intentos por IP por minuto.
 * Evita fuerza bruta sin depender de infraestructura externa.
 */
@Component
public class LoginRateLimiter {

    private static final int MAX_ATTEMPTS = 5;
    private static final Duration WINDOW = Duration.ofMinutes(1);

    private final ConcurrentHashMap<String, Deque<Instant>> attempts = new ConcurrentHashMap<>();

    public boolean isAllowed(String key) {
        Deque<Instant> deque = attempts.computeIfAbsent(key, k -> new ConcurrentLinkedDeque<>());
        Instant now = Instant.now();
        // Limpiar ventana
        while (!deque.isEmpty() && deque.peekFirst().isBefore(now.minus(WINDOW))) {
            deque.pollFirst();
        }
        if (deque.size() >= MAX_ATTEMPTS) {
            return false;
        }
        deque.addLast(now);
        return true;
    }

    public void reset(String key) {
        attempts.remove(key);
    }
}
