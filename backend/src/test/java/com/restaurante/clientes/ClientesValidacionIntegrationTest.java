package com.restaurante.clientes;

import com.restaurante.AbstractIntegracionApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A-06: las líneas de pedido cliente se validan en cascada con restricciones de
 * tipo; entradas inválidas fallan con 400 controlado (no NPE ni 500).
 */
class ClientesValidacionIntegrationTest extends AbstractIntegracionApi {

    private String adminToken;
    private String clienteToken;

    @BeforeEach
    void preparar() {
        adminToken = tokenAdmin();
        clienteToken = asegurarCliente("cliente_val", "claveCli123");
    }

    @Test
    void rechazaCantidadCeroOAusente() throws Exception {
        long productoId = crearProducto("ProdVal0-" + System.nanoTime(), "6.00");
        String codigo = "CLI-V0-" + System.nanoTime();

        mockMvc.perform(formarPedido(codigo, productoId, "{\"productoId\":" + productoId + ",\"cantidad\":0}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(formarPedido("CLI-V1-" + System.nanoTime(), productoId, "{\"productoId\":" + productoId + "}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rechazaLineaNulaOSinProducto() throws Exception {
        long productoId = crearProducto("ProdVal1-" + System.nanoTime(), "6.00");

        mockMvc.perform(formarPedido("CLI-V2-" + System.nanoTime(), productoId, "null"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(formarPedido("CLI-V3-" + System.nanoTime(), productoId, "{\"cantidad\":1}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rechazaListaVaciaSinItemsOExtrasDemasiados() throws Exception {
        long productoId = crearProducto("ProdVal2-" + System.nanoTime(), "6.00");
        String codigo = "CLI-V4-" + System.nanoTime();

        mockMvc.perform(formarPedido(codigo, productoId, ""))
                .andExpect(status().isBadRequest());

        java.util.List<Long> extras = new java.util.ArrayList<>();
        for (int i = 0; i < 21; i++) {
            extras.add(900000L + i);
        }
        String item = "{\"productoId\":" + productoId + ",\"cantidad\":1,\"extraIds\":" + extras + "}";
        mockMvc.perform(formarPedido("CLI-V5-" + System.nanoTime(), productoId, item))
                .andExpect(status().isBadRequest());
    }

    @Test
    void pedidoValidoSigueCreandose() throws Exception {
        long productoId = crearProducto("ProdVal3-" + System.nanoTime(), "6.00");
        String codigo = "CLI-V6-" + System.nanoTime();
        String item = "{\"productoId\":" + productoId + ",\"cantidad\":1}";

        mockMvc.perform(formarPedido(codigo, productoId, item))
                .andExpect(status().isOk());
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder formarPedido(
            String codigo, long productoId, String item) throws Exception {
        String idem = "idem-" + System.nanoTime();
        String itemsJson = item.isBlank() ? "[]" : "[" + item + "]";
        String body = """
                {
                  "codigo":"%s",
                  "metodoPago":"EFECTIVO",
                  "metodoEntrega":"RETIRAR",
                  "idempotencyKey":"%s",
                  "items":%s
                }
                """.formatted(codigo, idem, itemsJson);
        return post("/api/v1/clientes/pedidos")
                .header("Authorization", "Bearer " + clienteToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
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