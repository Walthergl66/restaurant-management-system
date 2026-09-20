package com.restaurante.usuarios.infrastructure.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;

/**
 * Respuesta 401 en formato ProblemDetail cuando falta o es inválido el token.
 */
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final Logger log = LoggerFactory.getLogger(RestAuthenticationEntryPoint.class);

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException, ServletException {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED, "Autenticación requerida: envía un token JWT válido");
        problem.setType(URI.create("urn:problem:restaurante:no-autenticado"));
        problem.setTitle("Unauthorized");
        problem.setProperty("path", request.getRequestURI());

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        writeJson(response, problem);
    }

    static void writeJson(HttpServletResponse response, Object body) throws IOException {
        response.getWriter().write(toJson(body));
    }

    private static String toJson(Object value) {
        try {
            var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.writeValueAsString(value);
        } catch (Exception e) {
            log.error("No se pudo serializar ProblemDetail", e);
            return "{}";
        }
    }
}