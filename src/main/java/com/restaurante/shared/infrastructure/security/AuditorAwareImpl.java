package com.restaurante.shared.infrastructure.security;

import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * Resuelve el usuario actual para la auditoría. Cuando no hay sesión
 * autenticada (migraciones, procesos internos) registra "sistema".
 */
public class AuditorAwareImpl implements AuditorAware<String> {

    static final String UNKNOWN_USER = "sistema";

    @Override
    public Optional<String> getCurrentAuditor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.of(UNKNOWN_USER);
        }
        if ("anonymousUser".equals(authentication.getPrincipal())) {
            return Optional.of(UNKNOWN_USER);
        }
        return Optional.of(authentication.getName());
    }
}