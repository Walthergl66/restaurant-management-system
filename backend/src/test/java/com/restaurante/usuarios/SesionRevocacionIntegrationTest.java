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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A-04: un cambio administrativo de credenciales, rol o estado revoca de
 * inmediato las sesiones (access token vía versión de sesión y refresh tokens
 * por revocación directa).
 */
@AutoConfigureMockMvc
class SesionRevocacionIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void cambiarPasswordRevocaAccesoYRefresh() throws Exception {
        String uid = String.valueOf(System.nanoTime());
        String username = "revoca_pwd_" + uid;
        Long id = crearUsuario(username, "MESERO");

        String[] tokens = loginTokens(username, "clave123");
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + tokens[0]))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/v1/usuarios/{id}/password", id)
                        .header("Authorization", "Bearer " + loginToken("admin", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nuevaPassword":"nueva456"}
                                """))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + tokens[0]))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"%s"}
                                """.formatted(tokens[1])))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + loginToken(username, "nueva456")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(username));
    }

    @Test
    void cambiarRolRevocaAccesoYRefresh() throws Exception {
        String uid = String.valueOf(System.nanoTime());
        String username = "revoca_rol_" + uid;
        Long id = crearUsuario(username, "MESERO");

        String[] tokens = loginTokens(username, "clave123");
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + tokens[0]))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/v1/usuarios/{id}", id)
                        .header("Authorization", "Bearer " + loginToken("admin", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Usuario Prueba","rolCodigo":"CAJERO"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol").value("CAJERO"));

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + tokens[0]))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"%s"}
                                """.formatted(tokens[1])))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void desactivarReactivarInvalidaSesionesViejas() throws Exception {
        String uid = String.valueOf(System.nanoTime());
        String username = "revoca_estado_" + uid;
        Long id = crearUsuario(username, "MESERO");

        String[] tokens = loginTokens(username, "clave123");
        String adminToken = loginToken("admin", "admin123");

        mockMvc.perform(put("/api/v1/usuarios/{id}", id)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Usuario Prueba","activo":false}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false));

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + tokens[0]))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"%s"}
                                """.formatted(tokens[1])))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"clave123"}
                                """.formatted(username)))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(put("/api/v1/usuarios/{id}", id)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Usuario Prueba","activo":true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(true));

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"%s"}
                                """.formatted(tokens[1])))
                .andExpect(status().isUnauthorized());

        String nuevoAcceso = loginToken(username, "clave123");
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + nuevoAcceso))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(username));
    }

    private Long crearUsuario(String username, String rolCodigo) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/usuarios")
                        .header("Authorization", "Bearer " + loginToken("admin", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","nombre":"Usuario Prueba","password":"clave123","rolCodigo":"%s"}
                                """.formatted(username, rolCodigo)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("id").asLong();
    }

    private String loginToken(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"%s"}
                                """.formatted(username, password)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("accessToken").asText();
    }

    private String[] loginTokens(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"%s"}
                                """.formatted(username, password)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return new String[]{body.path("accessToken").asText(), body.path("refreshToken").asText()};
    }
}