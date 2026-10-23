package com.restaurante.usuarios.infrastructure.security;

import com.restaurante.auditoria.Auditoria;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * RNF-07: desde que el registro y el reenvío consumen cuota del proveedor de
 * correo, las cuatro rutas públicas sensibles (login, refresh, registro y
 * reenviar-verificacion) comparten el límite de 5/min por IP; el resto del
 * auth (verificar-email) y los GET no se acotan.
 */
class LoginRateLimitFilterTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void limitaRegistroYReenviarVerificacionPorIp() throws Exception {
        for (String ruta : new String[]{"/api/v1/auth/registro", "/api/v1/auth/reenviar-verificacion"}) {
            LoginRateLimitFilter filter = filtroConCuotaAgotada();

            MockHttpServletRequest request = new MockHttpServletRequest("POST", ruta);
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockFilterChain chain = new MockFilterChain();

            filter.doFilter(request, response, chain);

            assertEquals(429, response.getStatus(), ruta + " debe quedar acotada");
            assertNull(chain.getRequest(), "la petición no debe pasar al filtro siguiente");
        }
    }

    @Test
    void sigueLimitandoLoginYRefresh() throws Exception {
        for (String ruta : new String[]{"/api/v1/auth/login", "/api/v1/auth/refresh"}) {
            LoginRateLimitFilter filter = filtroConCuotaAgotada();

            MockHttpServletRequest request = new MockHttpServletRequest("POST", ruta);
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilter(request, response, new MockFilterChain());

            assertEquals(429, response.getStatus(), ruta + " debe seguir acotada");
        }
    }

    @Test
    void noLimitaVerificarEmailNiPeticionesGet() throws Exception {
        LoginRateLimitFilter filter = filtroConCuotaAgotada();

        MockHttpServletRequest postVerificar = new MockHttpServletRequest("POST", "/api/v1/auth/verificar-email");
        MockHttpServletResponse respuestaVerificar = new MockHttpServletResponse();
        MockFilterChain chainVerificar = new MockFilterChain();
        filter.doFilter(postVerificar, respuestaVerificar, chainVerificar);
        assertEquals(200, respuestaVerificar.getStatus());
        assertSame(postVerificar, chainVerificar.getRequest());

        MockHttpServletRequest getRegistro = new MockHttpServletRequest("GET", "/api/v1/auth/registro");
        MockHttpServletResponse respuestaGet = new MockHttpServletResponse();
        MockFilterChain chainGet = new MockFilterChain();
        filter.doFilter(getRegistro, respuestaGet, chainGet);
        assertEquals(200, respuestaGet.getStatus());
        assertSame(getRegistro, chainGet.getRequest());
    }

    /** Filtro activo (MockEnvironment sin perfil test) con la IP ya agotada. */
    private LoginRateLimitFilter filtroConCuotaAgotada() {
        LoginRateLimiter limiter = new LoginRateLimiter();
        for (int i = 0; i < LoginRateLimiter.MAX_ATTEMPTS; i++) {
            limiter.isAllowed("127.0.0.1");
        }
        return new LoginRateLimitFilter(limiter, objectMapper, new MockEnvironment(), auditoriaNula());
    }

    @SuppressWarnings("unchecked")
    private static ObjectProvider<Auditoria> auditoriaNula() {
        // getIfAvailable() devuelve null por defecto: el filtro tolera
        // auditoría ausente, igual que en el arranque mínimo.
        return org.mockito.Mockito.mock(ObjectProvider.class);
    }
}
