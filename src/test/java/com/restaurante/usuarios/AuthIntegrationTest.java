package com.restaurante.usuarios;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import com.restaurante.AbstractIntegrationTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class AuthIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Nested
    class LoginYToken {

        @Test
        void logueaConAdminDelSeed() throws Exception {
            MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"username":"admin","password":"admin123"}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.tokenType").value("Bearer"))
                    .andExpect(jsonPath("$.accessToken").isNotEmpty())
                    .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                    .andExpect(jsonPath("$.expiresInSeconds").isNumber())
                    .andExpect(jsonPath("$.usuario.username").value("admin"))
                    .andExpect(jsonPath("$.usuario.rol").value("ADMIN"))
                    .andExpect(jsonPath("$.usuario.permisos").isArray())
                    .andReturn();

            JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
            mockMvc.perform(get("/api/v1/auth/me")
                            .header("Authorization", "Bearer " + body.path("accessToken").asText()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.username").value("admin"));
        }

        @Test
        void identificaSesionConToken() throws Exception {
            String token = loginToken("admin", "admin123");
            mockMvc.perform(get("/api/v1/auth/me")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.username").value("admin"));
        }

        @Test
        void rechazaCredencialesIncorrectas() throws Exception {
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"username":"admin","password":"incorrecta"}
                                    """))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.type").value("urn:problem:restaurante:no-autenticado"));
        }

        @Test
        void rechazaPeticionesSinToken() throws Exception {
            mockMvc.perform(get("/api/v1/auth/me"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void renuevaAccesoConRefreshToken() throws Exception {
            String refresh = loginRefreshToken("admin", "admin123");
            MvcResult result = mockMvc.perform(post("/api/v1/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"refreshToken":"%s"}
                                    """.formatted(refresh)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").isNotEmpty())
                    .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                    .andReturn();

            JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
            mockMvc.perform(get("/api/v1/auth/me")
                            .header("Authorization", "Bearer " + body.path("accessToken").asText()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.username").value("admin"));
        }

        @Test
        void revocaRefreshTokenAlCerrarSesion() throws Exception {
            String[] par = loginTokens("admin", "admin123");
            String access = par[0];
            String refresh = par[1];
            mockMvc.perform(post("/api/v1/auth/logout")
                            .header("Authorization", "Bearer " + access)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"refreshToken":"%s"}
                                    """.formatted(refresh)))
                    .andExpect(status().isNoContent());

            mockMvc.perform(post("/api/v1/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"refreshToken":"%s"}
                                    """.formatted(refresh)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void rechazaRefreshTokenInvalido() throws Exception {
            mockMvc.perform(post("/api/v1/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"refreshToken":"uuid-inexistente"}
                                    """))
                    .andExpect(status().isUnauthorized());
        }

        private String loginToken(String username, String password) throws Exception {
            return loginTokens(username, password)[0];
        }

        private String loginRefreshToken(String username, String password) throws Exception {
            return loginTokens(username, password)[1];
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

    @Nested
    class PermisosPorMetodo {

        @Test
        void creaUsuarioSoloConPermisoDeAdministracion() throws Exception {
            String adminToken = login("admin", "admin123");
            String mesero = "mesero_" + System.nanoTime();

            // Se crea un MESERO con el token de administrador (permiso usuarios:crear)
            mockMvc.perform(post("/api/v1/usuarios")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"username":"%s","nombre":"Mesero Prueba","password":"clave123","rolCodigo":"MESERO"}
                                    """.formatted(mesero)))
                    .andExpect(status().isCreated());

            // El MESERO no puede crear usuarios → 403 con ProblemDetail
            String meseroToken = login(mesero, "clave123");
            mockMvc.perform(post("/api/v1/usuarios")
                            .header("Authorization", "Bearer " + meseroToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"username":"otro","nombre":"Otro","password":"clave123","rolCodigo":"MESERO"}
                                    """))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.type").value("urn:problem:restaurante:acceso-denegado"));

            // El MESERO sí puede ver su propia sesión
            mockMvc.perform(get("/api/v1/auth/me")
                            .header("Authorization", "Bearer " + meseroToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.rol").value("MESERO"));
        }

        @Test
        void noPuedeDuplicarUsuario() throws Exception {
            String adminToken = login("admin", "admin123");
            mockMvc.perform(post("/api/v1/usuarios")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"username":"admin","nombre":"Prueba","password":"clave123","rolCodigo":"MESERO"}
                                    """))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.type").value("urn:problem:restaurante:conflicto"));
        }

        private String login(String username, String password) throws Exception {
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
    }
}