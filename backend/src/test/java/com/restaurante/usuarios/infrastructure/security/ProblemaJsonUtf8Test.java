package com.restaurante.usuarios.infrastructure.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Las respuestas de error que escriben los filtros y handlers de Spring
 * Security a mano deben salir en UTF-8: el JSON es UTF-8 por definición
 * (RFC 8259) y sin fijar la codificación el contenedor usa ISO-8859-1, lo que
 * deja los acentos de los mensajes como bytes inválidos.
 */
class ProblemaJsonUtf8Test {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void laRespuesta401SeEscribeEnUtf8() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/usuarios/roles");
        MockHttpServletResponse response = new MockHttpServletResponse();

        new RestAuthenticationEntryPoint(objectMapper)
                .commence(request, response, new InsufficientAuthenticationException("sin token"));

        assertEquals(401, response.getStatus());
        assertUtf8ConAcentos(response, "Autenticación requerida: envía un token JWT válido");
    }

    @Test
    void laRespuesta403SeEscribeEnUtf8() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/usuarios");
        MockHttpServletResponse response = new MockHttpServletResponse();

        new RestAccessDeniedHandler(objectMapper)
                .handle(request, response, new AccessDeniedException("sin permiso"));

        assertEquals(403, response.getStatus());
        assertUtf8ConAcentos(response, "No tienes permiso para esta operación");
    }

    @Test
    void laRespuesta429PorDemasiadosIntentosSeEscribeEnUtf8() throws Exception {
        LoginRateLimiter limiter = new LoginRateLimiter();
        for (int i = 0; i < LoginRateLimiter.MAX_ATTEMPTS; i++) {
            limiter.isAllowed("127.0.0.1");
        }
        org.springframework.beans.factory.ObjectProvider<com.restaurante.auditoria.Auditoria> provider =
                new org.springframework.beans.factory.ObjectProvider<com.restaurante.auditoria.Auditoria>() {
                    public com.restaurante.auditoria.Auditoria getObject() { return null; }
                    public com.restaurante.auditoria.Auditoria getIfAvailable() { return null; }
                    public com.restaurante.auditoria.Auditoria getIfUnique() { return null; }
                    public com.restaurante.auditoria.Auditoria getObject(Object... args) { return null; }
                    public java.util.stream.Stream<com.restaurante.auditoria.Auditoria> stream() { return java.util.stream.Stream.empty(); }
                    public java.util.stream.Stream<com.restaurante.auditoria.Auditoria> orderedStream() { return java.util.stream.Stream.empty(); }
                    public java.util.Iterator<com.restaurante.auditoria.Auditoria> iterator() { return java.util.Collections.emptyIterator(); }
                };
        LoginRateLimitFilter filter = new LoginRateLimitFilter(limiter, objectMapper, new MockEnvironment(), provider);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertEquals(429, response.getStatus());
        assertUtf8ConAcentos(response, "Demasiados intentos, intente más tarde");
    }

    /**
     * El cuerpo debe ser UTF-8 válido y el contenedor debe declararlo: si el
     * charset no se fija, la respuesta sale en ISO-8859-1 y los acentos salen
     * como caracteres de reemplazo.
     */
    private void assertUtf8ConAcentos(MockHttpServletResponse response, String textoEsperado) throws Exception {
        assertTrue(response.getContentType().contains("charset=UTF-8"),
                "el Content-Type debe declarar UTF-8: " + response.getContentType());

        String body = new String(response.getContentAsByteArray(), StandardCharsets.UTF_8);
        assertTrue(body.contains(textoEsperado), "cuerpo inesperado: " + body);
        assertEquals(body, response.getContentAsString(),
                "el charset declarado debe coincidir con los bytes escritos");
    }
}
