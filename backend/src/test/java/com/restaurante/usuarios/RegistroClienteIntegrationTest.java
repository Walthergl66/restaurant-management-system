package com.restaurante.usuarios;

import com.restaurante.AbstractIntegrationTest;
import com.restaurante.SoporteRegistro;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * RF-45: auto-registro público de clientes con verificación de correo. Crea el
 * usuario con rol CLIENTE pendiente de verificar; sólo tras confirmar el código
 * enviado al correo queda habilitado el inicio de sesión.
 */
@AutoConfigureMockMvc
class RegistroClienteIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private EmisorCorreo emisorCorreo;

    @Test
    void registraClienteExigeVerificacionYActivaLaCuenta() throws Exception {
        long semilla = System.nanoTime();
        String correo = SoporteRegistro.correo(semilla);
        String cedula = SoporteRegistro.cedula(semilla);
        String celular = SoporteRegistro.celular(semilla);

        mockMvc.perform(post("/api/v1/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoRegistro(correo, cedula, celular, "Clave#123")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.emailVerificado").value(false));

        // Todavía no puede iniciar sesión: el correo está sin verificar.
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"Clave#123"}
                                """.formatted(correo)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type").value("urn:problem:restaurante:correo-no-verificado"));

        // El código sale por el puerto de correo (mock capturado).
        ArgumentCaptor<String> codigo = ArgumentCaptor.forClass(String.class);
        verify(emisorCorreo).enviarCodigoVerificacion(eq(correo), codigo.capture());

        // Verificar el correo deja la sesión iniciada.
        String body = mockMvc.perform(post("/api/v1/auth/verificar-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","codigo":"%s"}
                                """.formatted(correo, codigo.getValue())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.usuario.username").value(correo))
                .andExpect(jsonPath("$.usuario.rol").value("CLIENTE"))
                .andReturn().getResponse().getContentAsString();

        String accessToken = objectMapper.readTree(body).path("accessToken").asText();
        mockMvc.perform(get("/api/v1/menu")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        // Y ahora sí puede iniciar sesión normalmente.
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"Clave#123"}
                                """.formatted(correo)))
                .andExpect(status().isOk());
    }

    @Test
    void reenviaElCodigoYElUltimoEsElValido() throws Exception {
        long semilla = System.nanoTime();
        String correo = SoporteRegistro.correo(semilla);

        mockMvc.perform(post("/api/v1/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoRegistro(correo, SoporteRegistro.cedula(semilla),
                                SoporteRegistro.celular(semilla), "Clave#123")))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/auth/reenviar-verificacion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s"}
                                """.formatted(correo)))
                .andExpect(status().isNoContent());

        ArgumentCaptor<String> codigos = ArgumentCaptor.forClass(String.class);
        verify(emisorCorreo, org.mockito.Mockito.times(2))
                .enviarCodigoVerificacion(eq(correo), codigos.capture());
        String ultimo = codigos.getAllValues().get(1);

        mockMvc.perform(post("/api/v1/auth/verificar-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","codigo":"%s"}
                                """.formatted(correo, ultimo)))
                .andExpect(status().isOk());
    }

    @Test
    void rechazaUsuarioDuplicado() throws Exception {
        long semilla = System.nanoTime();
        String correo = SoporteRegistro.correo(semilla);
        String cuerpo = cuerpoRegistro(correo, SoporteRegistro.cedula(semilla),
                SoporteRegistro.celular(semilla), "Clave#123");

        mockMvc.perform(post("/api/v1/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("urn:problem:restaurante:conflicto"));
    }

    @Test
    void rechazaCedulaDuplicadaEnOtroCorreo() throws Exception {
        long semilla = System.nanoTime();
        String cedula = SoporteRegistro.cedula(semilla);

        mockMvc.perform(post("/api/v1/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoRegistro(SoporteRegistro.correo(semilla), cedula,
                                SoporteRegistro.celular(semilla), "Clave#123")))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoRegistro(SoporteRegistro.correo(semilla + 7), cedula,
                                SoporteRegistro.celular(semilla + 9), "Clave#123")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("urn:problem:restaurante:conflicto"));
    }

    @Test
    void rechazaPasswordDebil() throws Exception {
        long semilla = System.nanoTime();
        mockMvc.perform(post("/api/v1/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoRegistro(SoporteRegistro.correo(semilla),
                                SoporteRegistro.cedula(semilla), SoporteRegistro.celular(semilla),
                                "clave123")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rechazaCedulaInvalida() throws Exception {
        long semilla = System.nanoTime();
        mockMvc.perform(post("/api/v1/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoRegistro(SoporteRegistro.correo(semilla), "1234567890",
                                SoporteRegistro.celular(semilla), "Clave#123")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rechazaCorreoInvalido() throws Exception {
        long semilla = System.nanoTime();
        mockMvc.perform(post("/api/v1/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoRegistro("no-es-correo", SoporteRegistro.cedula(semilla),
                                SoporteRegistro.celular(semilla), "Clave#123")))
                .andExpect(status().isBadRequest());
    }

    private static String cuerpoRegistro(String correo, String cedula, String celular, String password) {
        return """
                {"username":"%s","nombre":"Cliente Prueba","cedula":"%s","celular":"%s","password":"%s"}
                """.formatted(correo, cedula, celular, password);
    }
}
