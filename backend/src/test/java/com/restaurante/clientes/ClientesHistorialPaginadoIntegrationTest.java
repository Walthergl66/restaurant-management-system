package com.restaurante.clientes;

import com.restaurante.AbstractIntegracionApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** A-10: el historial del cliente está paginado (últimos primero) y cada página
 *  trae sus líneas sin N+1. */
class ClientesHistorialPaginadoIntegrationTest extends AbstractIntegracionApi {

    private String adminToken;
    private String clienteToken;

    @BeforeEach
    void preparar() {
        adminToken = tokenAdmin();
        clienteToken = asegurarCliente("cliente_hist", "claveCli123");
    }

    @Test
    void historialPaginaDeMasRecienteAMasAntiguo() throws Exception {
        long productoId = crearProducto("ProdHist-" + System.nanoTime(), "4.00");
        List<String> codigos = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            String codigo = "CLI-HIST-" + System.nanoTime();
            codigos.add(codigo);
            mockMvc.perform(post("/api/v1/clientes/pedidos")
                            .header("Authorization", "Bearer " + clienteToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "codigo":"%s",
                                      "metodoPago":"EFECTIVO",
                                      "metodoEntrega":"RETIRAR",
                                      "idempotencyKey":"idem-%s",
                                      "items":[{"productoId":%d,"cantidad":1,"extraIds":[],"ingredientesRemovidos":[]}]
                                    }
                                    """.formatted(codigo, System.nanoTime(), productoId)))
                    .andExpect(status().isOk());
        }

        String pagina0 = mockMvc.perform(get("/api/v1/clientes/historial?page=0&size=2")
                        .header("Authorization", "Bearer " + clienteToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(2, objectMapper.readTree(pagina0).path("content").size());
        assertEquals(3, objectMapper.readTree(pagina0).path("totalElements").asInt());
        assertEquals(codigos.get(2), objectMapper.readTree(pagina0).path("content").get(0).path("codigo").asText(),
                "el más reciente debe ir primero");
        assertTrue(objectMapper.readTree(pagina0).path("content").get(0).path("lineas").size() >= 1,
                "las líneas deben venir cargadas en la página");

        String pagina1 = mockMvc.perform(get("/api/v1/clientes/historial?page=1&size=2")
                        .header("Authorization", "Bearer " + clienteToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertEquals(1, objectMapper.readTree(pagina1).path("content").size());
        assertEquals(codigos.get(0), objectMapper.readTree(pagina1).path("content").get(0).path("codigo").asText());
    }

    private long crearProducto(String nombre, String precio) throws Exception {
        String body = mockMvc.perform(post("/api/v1/productos")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"%s","precio":%s,"extraIds":[],"ingredientes":["cebolla"]}
                                """.formatted(nombre, precio)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).path("id").asLong();
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