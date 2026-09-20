package com.restaurante;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Base de las pruebas de integración sobre la API: MockMvc, Jackson 3 y
 * utilidades de autenticación para los tests.
 */
@AutoConfigureMockMvc
public abstract class AbstractIntegracionApi extends AbstractIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    protected String tokenAdmin() {
        return loginToken("admin", "admin123");
    }

    protected String loginToken(String username, String password) {
        MvcResult result = null;
        try {
            result = mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"username":"%s","password":"%s"}
                                    """.formatted(username, password)))
                    .andExpect(status().isOk())
                    .andReturn();
            return objectMapper.readTree(result.getResponse().getContentAsString())
                    .path("accessToken").asText();
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo autenticar " + username, e);
        }
    }

    /**
     * Crea (si no existe) un usuario mesero vía API y devuelve su token.
     * Idempotente: el contenedor compartido persiste entre clases de test.
     */
    protected String tokenMesero() {
        try {
            mockMvc.perform(post("/api/v1/usuarios")
                            .header("Authorization", "Bearer " + tokenAdmin())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"username":"mesero_test","nombre":"Mesero Test","password":"clave123","rolCodigo":"MESERO"}
                                    """))
                    .andExpect(result -> {
                        int status = result.getResponse().getStatus();
                        if (status != 201 && status != 409) {
                            throw new AssertionError("Esperaba 201 o 409 al crear el mesero, fue " + status);
                        }
                    });
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo asegurar el usuario mesero", e);
        }
        return loginToken("mesero_test", "clave123");
    }
}