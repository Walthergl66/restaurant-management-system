package com.restaurante.usuarios.infrastructure.security;

import com.restaurante.usuarios.JwtValidador;
import com.restaurante.usuarios.infrastructure.UsuarioRepository;
import io.jsonwebtoken.Claims;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Implementación del SPI {@link JwtValidador}: reutiliza {@link JwtService} para
 * firma/vigencia y comprueba en base la versión de sesión (A-04), de modo que el
 * resto de la aplicación autentica con la misma política que el filtro HTTP.
 */
@Component
public class JwtValidadorImpl implements JwtValidador {

    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;

    public JwtValidadorImpl(JwtService jwtService, UsuarioRepository usuarioRepository) {
        this.jwtService = jwtService;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public Optional<AccesoValido> validar(String bearerToken) {
        if (bearerToken == null || !bearerToken.startsWith(BEARER_PREFIX)) {
            return Optional.empty();
        }
        Claims claims = jwtService.validar(bearerToken.substring(BEARER_PREFIX.length()));
        if (claims == null || !sesionValida(claims)) {
            return Optional.empty();
        }
        List<String> permisos = Optional.ofNullable(claims.get(JwtService.CLAIM_AUTHORITIES, List.class))
                .orElse(List.of())
                .stream()
                .map(String::valueOf)
                .toList();
        return Optional.of(new AccesoValido(claims.getSubject(),
                claims.get(JwtService.CLAIM_ROL, String.class), permisos));
    }

    private boolean sesionValida(Claims claims) {
        Object versionEnToken = claims.get(JwtService.CLAIM_SESION_VERSION);
        if (!(versionEnToken instanceof Number numero)) {
            return false;
        }
        Optional<Long> actual = usuarioRepository.findSesionVersionActivo(claims.getSubject());
        return actual.isPresent() && actual.get() == numero.longValue();
    }

    private static final String BEARER_PREFIX = "Bearer ";
}