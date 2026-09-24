package com.restaurante.usuarios.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;

/**
 * Filtro que limita intentos de login y renovación de sesión por IP — RNF-07.
 * A-07: aplica el mismo límite a refresh para no dejar una vía abierta de
 * fuerza bruta junto al login.
 */
@Component
public class LoginRateLimitFilter extends OncePerRequestFilter {

    private final LoginRateLimiter rateLimiter;
    private final ObjectMapper objectMapper;
    private final Environment environment;

    public LoginRateLimitFilter(LoginRateLimiter rateLimiter, ObjectMapper objectMapper, Environment environment) {
        this.rateLimiter = rateLimiter;
        this.objectMapper = objectMapper;
        this.environment = environment;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        // Deshabilitar en perfil test para no romper Testcontainers
        if (Arrays.asList(environment.getActiveProfiles()).contains("test")) {
            filterChain.doFilter(request, response);
            return;
        }
        if ("POST".equalsIgnoreCase(request.getMethod())
                && ("/api/v1/auth/login".equals(request.getRequestURI())
                || "/api/v1/auth/refresh".equals(request.getRequestURI()))) {
            String ip = request.getRemoteAddr();
            String key = ip != null ? ip : "unknown";
            // X-Forwarded-For solo se confía tras ForwardedHeaderFilter con proxy confiable; por defecto se ignora para evitar spoof
            if (!rateLimiter.isAllowed(key)) {
                ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS,
                        "Demasiados intentos de login, intente más tarde");
                problem.setType(URI.create("urn:problem:restaurante:rate-limit"));
                problem.setTitle("Too Many Requests");
                problem.setProperty("path", request.getRequestURI());
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.getWriter().write(objectMapper.writeValueAsString(problem));
                return;
            }
        }
        filterChain.doFilter(request, response);
    }
}
