package com.restaurante.usuarios;

import com.restaurante.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Gestión directa de usuarios desde la lista: borrado definitivo (DELETE) y
 * activación/desactivación (PATCH /estado). El borrado se rechaza con 409 si
 * el usuario tiene perfil de cliente (FK en comercial.clientes sin cascada).
 */
@AutoConfigureMockMvc
class UsuarioGestionIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void eliminarDefinitivoBorraElUsuarioYSusSesiones() throws Exception {
        String username = "borra_def_" + System.nanoTime() % 1_000_000_000L;
        Long id = crearUsuario(username, "MESERO");

        mockMvc.perform(delete("/api/v1/usuarios/{id}", id)
                        .header("Authorization", "Bearer " + loginToken("admin", "admin123")))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/usuarios/{id}", id)
                        .header("Authorization", "Bearer " + loginToken("admin", "admin123")))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"clave123"}
                                """.formatted(username)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void eliminarUsuarioConPerfilClienteDevuelve409YNoBorra() throws Exception {
        String username = "borra_cli_" + System.nanoTime() % 1_000_000_000L;
        Long id = crearUsuario(username, "MESERO");
        String uid = String.valueOf(System.nanoTime() % 1_000_000_000L);
        jdbc.update("""
                        INSERT INTO comercial.clientes (usuario_id, cedula, telefono, nombre, version, creado_at, actualizado_at)
                        VALUES (?, ?, ?, ?, 0, now(), now())
                        """,
                id, "ced" + uid, "tel" + uid, "Cliente Prueba");

        mockMvc.perform(delete("/api/v1/usuarios/{id}", id)
                        .header("Authorization", "Bearer " + loginToken("admin", "admin123")))
                .andExpect(status().isConflict());

        mockMvc.perform(get("/api/v1/usuarios/{id}", id)
                        .header("Authorization", "Bearer " + loginToken("admin", "admin123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(username));
    }

    @Test
    void cambiarEstadoActivaYDesactivaSinPasarPorElModal() throws Exception {
        String username = "estado_dir_" + System.nanoTime() % 1_000_000_000L;
        Long id = crearUsuario(username, "MESERO");
        String adminToken = loginToken("admin", "admin123");

        mockMvc.perform(patch("/api/v1/usuarios/{id}/estado", id)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"activo":false}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"clave123"}
                                """.formatted(username)))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(patch("/api/v1/usuarios/{id}/estado", id)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"activo":true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(true));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"clave123"}
                                """.formatted(username)))
                .andExpect(status().isOk());
    }

    @Test
    void meseroNoPuedeEliminarNiCambiarEstado() throws Exception {
        String uid = String.valueOf(System.nanoTime() % 1_000_000_000L);
        String meseroUsername = "mesero_perm_" + uid;
        Long meseroId = crearUsuario(meseroUsername, "MESERO");
        String victimaUsername = "victima_" + uid;
        Long victimaId = crearUsuario(victimaUsername, "COCINA");
        String meseroToken = loginToken(meseroUsername, "clave123");

        mockMvc.perform(delete("/api/v1/usuarios/{id}", victimaId)
                        .header("Authorization", "Bearer " + meseroToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/v1/usuarios/{id}/estado", victimaId)
                        .header("Authorization", "Bearer " + meseroToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"activo":false}
                                """))
                .andExpect(status().isForbidden());
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
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("id").asLong();
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
}
