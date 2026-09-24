package com.restaurante.usuarios.infrastructure.security;

import com.restaurante.usuarios.infrastructure.UsuarioRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Filtro que autentica cada petición a partir del JWT del header
 * {@code Authorization: Bearer <token>}. A-04: además de validar firma y
 * vigencia, comprueba en base que la versión de sesión del usuario coincida
 * con la embebida en el token, de modo que cambiar la contraseña, el rol o el
 * estado revoca de inmediato los access tokens ya emitidos.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;

    public JwtAuthenticationFilter(JwtService jwtService, UsuarioRepository usuarioRepository) {
        this.jwtService = jwtService;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = extractBearerToken(request);
        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            Claims claims = jwtService.validar(token);
            if (claims != null && sesionValida(claims)) {
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                claims.getSubject(),
                                null,
                                authoritiesOf(claims));
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }
        filterChain.doFilter(request, response);
    }

    private boolean sesionValida(Claims claims) {
        Object versioneEm = claims.get(JwtService.CLAIM_SESION_VERSION);
        if (!(versioneEm instanceof Number numero)) {
            return false;
        }
        Optional<Long> actual = usuarioRepository.findSesionVersionActivo(claims.getSubject());
        return actual.isPresent() && actual.get() == numero.longValue();
    }

    private static List<SimpleGrantedAuthority> authoritiesOf(Claims claims) {
        List<String> permisos = Optional.ofNullable(claims.get(JwtService.CLAIM_AUTHORITIES, List.class))
                .orElse(List.of())
                .stream()
                .map(String::valueOf)
                .toList();
        String rol = claims.get(JwtService.CLAIM_ROL, String.class);
        java.util.List<SimpleGrantedAuthority> autoridades = permisos.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(java.util.stream.Collectors.toList());
        if (rol != null && !rol.isBlank()) {
            autoridades.add(new SimpleGrantedAuthority("ROLE_" + rol));
        }
        return autoridades;
    }

    private static String extractBearerToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            return null;
        }
        return header.substring(7);
    }
}