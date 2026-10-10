package com.restaurante.usuarios;

import com.restaurante.AbstractIntegrationTest;
import com.restaurante.usuarios.domain.RecuperacionPassword;
import com.restaurante.usuarios.domain.Usuario;
import com.restaurante.usuarios.infrastructure.RecuperacionPasswordRepository;
import com.restaurante.usuarios.infrastructure.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * RF-45: flujo de recuperación de contraseña con token de un solo uso.
 */
@AutoConfigureMockMvc
class RecuperacionPasswordIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private RecuperacionPasswordRepository recuperacionRepository;

    @Test
    void solicitaRespondeSiempreNoContent() throws Exception {
        // Usuario existente
        String username = "reset_sol_" + System.nanoTime();
        registrar(username);

        mockMvc.perform(post("/api/v1/auth/solicitar-recuperacion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s"}
                                """.formatted(username)))
                .andExpect(status().isNoContent());

        // Usuario inexistente: misma respuesta (no revela existencia)
        mockMvc.perform(post("/api/v1/auth/solicitar-recuperacion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"no_existe_%d"}
                                """.formatted(System.nanoTime())))
                .andExpect(status().isNoContent());
    }

    @Test
    void restableceConTokenValidoYCambiaElLogin() throws Exception {
        String username = "reset_cam_" + System.nanoTime();
        registrar(username);

        Usuario usuario = usuarioRepository.findByUsername(username).orElseThrow();
        String token = "token-" + System.nanoTime();
        recuperacionRepository.save(new RecuperacionPassword(
                usuario.getId(), RecuperacionPassword.hash(token), Instant.now().plus(Duration.ofMinutes(30))));

        mockMvc.perform(post("/api/v1/auth/restablecer-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token":"%s","nuevaPassword":"nuevaClave1"}
                                """.formatted(token)))
                .andExpect(status().isNoContent());

        // La contraseña anterior ya no sirve
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"clave123"}
                                """.formatted(username)))
                .andExpect(status().isUnauthorized());

        // La nueva contraseña sí
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"nuevaClave1"}
                                """.formatted(username)))
                .andExpect(status().isOk());

        // El token es de un solo uso
        mockMvc.perform(post("/api/v1/auth/restablecer-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token":"%s","nuevaPassword":"otraClave1"}
                                """.formatted(token)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void rechazaTokenInvalido() throws Exception {
        mockMvc.perform(post("/api/v1/auth/restablecer-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token":"token-inexistente","nuevaPassword":"nuevaClave1"}
                                """))
                .andExpect(status().isUnprocessableEntity());
    }

    private void registrar(String username) throws Exception {
        mockMvc.perform(post("/api/v1/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","nombre":"Cliente Reset","password":"clave123"}
                                """.formatted(username)))
                .andExpect(status().isCreated());
    }
}
