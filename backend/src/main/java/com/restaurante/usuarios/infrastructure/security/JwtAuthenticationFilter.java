package com.restaurante.usuarios.infrastructure.security;

import com.restaurante.usuarios.JwtValidador;
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
import java.util.ArrayList;
import java.util.List;

/**
 * Filtro que autentica cada petición a partir del JWT del header
 * {@code Authorization: Bearer <token>}, delegando la validación al SPI
 * {@link JwtValidador} (firma, vigencia y versión de sesión — A-04), la misma
 * política que usa el canal STOMP.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtValidador jwtValidador;

    public JwtAuthenticationFilter(JwtValidador jwtValidador) {
        this.jwtValidador = jwtValidador;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            var acceso = jwtValidador.validar(request.getHeader("Authorization"));
            if (acceso.isPresent()) {
                List<SimpleGrantedAuthority> autoridades = new ArrayList<>();
                acceso.get().permisos().forEach(p -> autoridades.add(new SimpleGrantedAuthority(p)));
                if (acceso.get().rol() != null && !acceso.get().rol().isBlank()) {
                    autoridades.add(new SimpleGrantedAuthority("ROLE_" + acceso.get().rol()));
                }
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(acceso.get().username(), null, autoridades);
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }
        filterChain.doFilter(request, response);
    }
}