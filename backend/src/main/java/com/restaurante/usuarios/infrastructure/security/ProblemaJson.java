package com.restaurante.usuarios.infrastructure.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ProblemDetail;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Escribe un {@link ProblemDetail} directamente en la respuesta HTTP.
 *
 * <p>Los filtros y handlers de Spring Security escriben la respuesta a mano,
 * fuera del ciclo de los message converters de Spring. Si no se fija la
 * codificación, el contenedor escribe con ISO-8859-1 y los acentos de los
 * mensajes salen como bytes inválidos: el JSON es UTF-8 por definición
 * (RFC 8259) y un cliente que lo decodifique como UTF-8 ve �.
 */
final class ProblemaJson {

    private ProblemaJson() {
    }

    /**
     * Escribe el problema con el tipo de contenido dado y charset UTF-8.
     *
     * @param mediaType tipo de contenido de la respuesta (p. ej. {@code application/problem+json})
     */
    static void escribir(HttpServletResponse response, String mediaType, ProblemDetail problem,
                         ObjectMapper objectMapper) throws IOException {
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(mediaType + ";charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(problem));
    }
}
