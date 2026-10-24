package com.restaurante.usuarios;

import com.restaurante.AbstractIntegracionApi;
import com.restaurante.clientes.domain.Cliente;
import com.restaurante.clientes.infrastructure.ClienteRepository;
import com.restaurante.usuarios.domain.Usuario;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Teléfono OPCIONAL en el registro de cliente (RF-45): sin celular el registro
 * tiene éxito y el perfil queda con telefono NULL (sin placeholders); con
 * celular inválido se rechaza; dos clientes sin teléfono conviven (UNIQUE
 * admite múltiples NULL).
 */
class RegistroClienteTelefonoOpcionalIntegrationTest extends AbstractIntegracionApi {

    @Autowired
    private ClienteRepository clienteRepository;

    @Test
    void registroSinCelularDa201YTelefonoNull() throws Exception {
        String correo = "sin_tel_" + System.nanoTime() + "@mail.com";
        String cedula = "1710034065"; // cédula válida (Pichincha)

        mockMvc.perform(post("/api/v1/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","nombre":"Sin Telefono","cedula":"%s","password":"Clave1234!"}
                                """.formatted(correo, cedula)))
                .andExpect(status().isCreated());

        Cliente perfil = perfilDe(correo);
        assertNull(perfil.getTelefono(), "El teléfono debe quedar NULL cuando no se envía");
    }

    @Test
    void registroConCelularInvalidoEsRechazado() throws Exception {
        String correo = "tel_malo_" + System.nanoTime() + "@mail.com";

        mockMvc.perform(post("/api/v1/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","nombre":"Tel Malo","cedula":"1710034065","celular":"12345","password":"Clave1234!"}
                                """.formatted(correo)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void dosClientesSinTelefonoConvivenConUnique() throws Exception {
        String correoA = "null_a_" + System.nanoTime() + "@mail.com";
        String correoB = "null_b_" + System.nanoTime() + "@mail.com";

        mockMvc.perform(post("/api/v1/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","nombre":"Null A","cedula":"0926687856","password":"Clave1234!"}
                                """.formatted(correoA)))
                .andExpect(status().isCreated());

        // Un segundo cliente también sin teléfono: ambos NULL, el UNIQUE no choca.
        mockMvc.perform(post("/api/v1/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","nombre":"Null B","cedula":"1710034057","password":"Clave1234!"}
                                """.formatted(correoB)))
                .andExpect(status().isCreated());

        assertNull(perfilDe(correoA).getTelefono());
        assertNull(perfilDe(correoB).getTelefono());
    }

    private Cliente perfilDe(String username) {
        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("No se creó el usuario " + username));
        Cliente perfil = clienteRepository.findByUsuarioId(usuario.getId())
                .orElseThrow(() -> new IllegalStateException("No se materializó el perfil de " + username));
        assertNotNull(perfil);
        return perfil;
    }
}
