package com.restaurante.delivery.web;

import com.restaurante.delivery.domain.RutasNoDisponiblesException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

/**
 * Manejador de excepciones PROPIO del módulo delivery. Vive aquí (no en el
 * advice global de {@code shared}) a propósito: {@link RutasNoDisponiblesException}
 * pertenece al dominio de delivery y {@code shared} no puede depender de un
 * módulo de negocio (lo verifica ArquitecturaTests/ModularityTests). Así el
 * mapeo del fallo del proveedor de rutas queda donde el módulo lo posee.
 *
 * <p>Reproduce el mismo formato {@link ProblemDetail} (RFC 7807) que el advice
 * global: {@code type = urn:problem:restaurante:rutas-no-disponibles} y la
 * propiedad {@code path}. El mensaje NUNCA incluye la api-key de ORS.
 */
@RestControllerAdvice(basePackages = "com.restaurante.delivery")
public class DeliveryExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(DeliveryExceptionHandler.class);

    private static final String PROBLEM_BASE = "urn:problem:restaurante:";

    /**
     * El proveedor de rutas (OpenRouteService) no está disponible: timeout,
     * respuesta 4xx/5xx, cuerpo inesperado, api-key ausente u origen de reparto
     * sin configurar. No es un error de la petición del usuario (no es 4xx):
     * es la indisponibilidad de un servicio externo, de ahí el 503.
     */
    @ExceptionHandler(RutasNoDisponiblesException.class)
    public ResponseEntity<ProblemDetail> handleRutasNoDisponibles(
            RutasNoDisponiblesException ex, HttpServletRequest request) {
        log.warn("Rutas no disponibles: {}", ex.getMessage());
        HttpStatus status = HttpStatus.SERVICE_UNAVAILABLE;
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, ex.getMessage());
        problem.setType(URI.create(PROBLEM_BASE + "rutas-no-disponibles"));
        problem.setTitle(status.getReasonPhrase());
        problem.setProperty("path", request.getRequestURI());
        return ResponseEntity.status(status).body(problem);
    }
}
