package com.restaurante.clientes;

import com.restaurante.AbstractIntegracionApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * RF-42/RF-45: direcciones guardadas (listar) y métodos de pago del cliente.
 */
class ClientesPerfilIntegrationTest extends AbstractIntegracionApi {

    private String adminToken;
    private String clienteToken;

    @BeforeEach
    void preparar() {
        adminToken = tokenAdmin();
        clienteToken = asegurarCliente("cliente_perfil", "claveCli123");
    }

    @Test
    void listaDireccionesActivasDelCliente() throws Exception {
        String etiqueta = "Casa-" + System.nanoTime();
        mockMvc.perform(post("/api/v1/clientes/direcciones")
                        .header("Authorization", "Bearer " + clienteToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"etiqueta":"%s","direccion":"Calle 1","telefono":"0999999999"}
                                """.formatted(etiqueta)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/clientes/direcciones")
                        .header("Authorization", "Bearer " + clienteToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.etiqueta=='%s')]".formatted(etiqueta), hasSize(1)));
    }

    @Test
    void gestionaMetodosDePagoDelCliente() throws Exception {
        String alias = "Visa-" + System.nanoTime();

        // Alta (solo metadata)
        String body = mockMvc.perform(post("/api/v1/clientes/metodos-pago")
                        .header("Authorization", "Bearer " + clienteToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tipo":"tarjeta","alias":"%s","ultimos4":"4242","predeterminado":true}
                                """.formatted(alias)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("TARJETA"))
                .andExpect(jsonPath("$.alias").value(alias))
                .andExpect(jsonPath("$.ultimos4").value("4242"))
                .andExpect(jsonPath("$.predeterminado").value(true))
                .andReturn().getResponse().getContentAsString();

        long id = objectMapper.readTree(body).path("id").asLong();

        // Listado
        mockMvc.perform(get("/api/v1/clientes/metodos-pago")
                        .header("Authorization", "Bearer " + clienteToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.alias=='%s')]".formatted(alias), hasSize(1)));

        // Baja lógica
        mockMvc.perform(delete("/api/v1/clientes/metodos-pago/" + id)
                        .header("Authorization", "Bearer " + clienteToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/clientes/metodos-pago")
                        .header("Authorization", "Bearer " + clienteToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.alias=='%s')]".formatted(alias), hasSize(0)));
    }

    @Test
    void rechazaTipoDeMetodoDePagoNoSoportado() throws Exception {
        mockMvc.perform(post("/api/v1/clientes/metodos-pago")
                        .header("Authorization", "Bearer " + clienteToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tipo":"BITCOIN","alias":"cripto"}
                                """))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void exigePermisoDeClienteParaMetodosDePago() throws Exception {
        String meseroToken = tokenMesero();
        mockMvc.perform(get("/api/v1/clientes/metodos-pago")
                        .header("Authorization", "Bearer " + meseroToken))
                .andExpect(status().isForbidden());
    }

    private String asegurarCliente(String username, String password) {
        try {
            mockMvc.perform(post("/api/v1/usuarios")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"username":"%s","nombre":"%s","password":"%s","rolCodigo":"CLIENTE"}
                                    """.formatted(username, username, password)))
                    .andExpect(result -> {
                        int status = result.getResponse().getStatus();
                        if (status != 201 && status != 409) {
                            throw new AssertionError("Esperaba 201 o 409 al crear cliente, fue " + status);
                        }
                    });
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo asegurar cliente " + username, e);
        }
        return loginToken(username, password);
    }
}
