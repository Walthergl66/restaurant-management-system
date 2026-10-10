package com.restaurante.usuarios;

import com.restaurante.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * RF-45: auto-registro público de clientes. Crea el usuario con rol CLIENTE,
 * deja la sesión iniciada y no expone datos sensibles.
 */
@AutoConfigureMockMvc
class RegistroClienteIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void registraClienteYDevuelveSesionIniciada() throws Exception {
        String username = "cliente_" + System.nanoTime();
        MvcResult result = mockMvc.perform(post("/api/v1/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","nombre":"Cliente Prueba","password":"clave123"}
                                """.formatted(username)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.usuario.username").value(username))
                .andExpect(jsonPath("$.usuario.rol").value("CLIENTE"))
                .andReturn();

        String accessToken = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("accessToken").asText();

        // El cliente recién registrado ya puede consumir el menú de la app.
        mockMvc.perform(get("/api/v1/menu")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());
    }

    @Test
    void rechazaUsuarioDuplicado() throws Exception {
        String username = "cliente_dup_" + System.nanoTime();
        mockMvc.perform(post("/api/v1/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","nombre":"Cliente Dup","password":"clave123"}
                                """.formatted(username)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","nombre":"Cliente Dup","password":"clave123"}
                                """.formatted(username)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("urn:problem:restaurante:conflicto"));
    }

    @Test
    void rechazaPasswordCorta() throws Exception {
        mockMvc.perform(post("/api/v1/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"cliente_corto_%d","nombre":"Cliente Corto","password":"123"}
                                """.formatted(System.nanoTime())))
                .andExpect(status().isBadRequest());
    }
}
