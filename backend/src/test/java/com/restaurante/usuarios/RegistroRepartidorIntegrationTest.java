package com.restaurante.usuarios;

import com.restaurante.AbstractIntegracionApi;
import com.restaurante.SoporteRegistro;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Auto-registro público de repartidores (rol DELIVERY): la cuenta nace con su
 * celular de contacto y pendiente de verificar el correo. Hasta confirmar el
 * código no puede iniciar sesión.
 */
class RegistroRepartidorIntegrationTest extends AbstractIntegracionApi {

    @MockitoBean
    private EmisorCorreo emisorCorreo;

    @Test
    void registraRepartidorExigeVerificacionYActivaLaCuenta() throws Exception {
        long semilla = System.nanoTime();
        String correo = "repartidor_" + Math.abs(semilla) + "@test.com";
        String celular = SoporteRegistro.celular(semilla);

        mockMvc.perform(post("/api/v1/auth/registro-repartidor")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoRegistro(correo, celular, "Clave#123")))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/auth/verificar-email"))
                .andExpect(jsonPath("$.username").value(correo))
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

        // Verificar el correo deja la sesión iniciada con el rol DELIVERY.
        mockMvc.perform(post("/api/v1/auth/verificar-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","codigo":"%s"}
                                """.formatted(correo, codigo.getValue())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuario.username").value(correo))
                .andExpect(jsonPath("$.usuario.rol").value("DELIVERY"));

        // Y ahora sí puede iniciar sesión normalmente.
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"Clave#123"}
                                """.formatted(correo)))
                .andExpect(status().isOk());
    }

    @Test
    void guardaElCelularDeContactoDelRepartidor() throws Exception {
        long semilla = System.nanoTime();
        String correo = "repartidor_cel_" + Math.abs(semilla) + "@test.com";
        String celular = SoporteRegistro.celular(semilla);

        mockMvc.perform(post("/api/v1/auth/registro-repartidor")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoRegistro(correo, celular, "Clave#123")))
                .andExpect(status().isCreated());

        assertThat(usuarioRepository.findByUsername(correo).orElseThrow().getCelular())
                .isEqualTo(celular);
    }

    @Test
    void rechazaUsuarioDuplicado() throws Exception {
        long semilla = System.nanoTime();
        String correo = "repartidor_dup_" + Math.abs(semilla) + "@test.com";
        String cuerpo = cuerpoRegistro(correo, SoporteRegistro.celular(semilla), "Clave#123");

        mockMvc.perform(post("/api/v1/auth/registro-repartidor")
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/auth/registro-repartidor")
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("urn:problem:restaurante:conflicto"));
    }

    @Test
    void rechazaCelularInvalido() throws Exception {
        mockMvc.perform(post("/api/v1/auth/registro-repartidor")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoRegistro("repartidor_mal_" + Math.abs(System.nanoTime()) + "@test.com",
                                "12345", "Clave#123")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rechazaPasswordDebil() throws Exception {
        mockMvc.perform(post("/api/v1/auth/registro-repartidor")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoRegistro("repartidor_pw_" + Math.abs(System.nanoTime()) + "@test.com",
                                SoporteRegistro.celular(System.nanoTime()), "clave123")))
                .andExpect(status().isBadRequest());
    }

    private static String cuerpoRegistro(String correo, String celular, String password) {
        return """
                {"username":"%s","nombre":"Repartidor Prueba","celular":"%s","password":"%s"}
                """.formatted(correo, celular, password);
    }
}
