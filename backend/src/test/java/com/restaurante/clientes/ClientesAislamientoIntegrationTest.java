package com.restaurante.clientes;

import com.restaurante.AbstractIntegracionApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** A-01/A-09: aislamiento entre clientes por REST. Un cliente no puede
 *  confirmar ni ver estados de pedidos de otro (404 sin filtrar existencia),
 *  y las respuestas nunca exponen idempotencyKey/clienteId/direccionId. */
class ClientesAislamientoIntegrationTest extends AbstractIntegracionApi {

    private String tokenA;
    private String tokenB;
    private String adminToken;

    @BeforeEach
    void preparar() {
        adminToken = tokenAdmin();
        tokenA = asegurarCliente("cli_a_aislamiento", "claveA123");
        tokenB = asegurarCliente("cli_b_aislamiento", "claveB123");
    }

    @Test
    void clienteBNoPuedeConfirmarPedidoDeClienteA() throws Exception {
        long productoId = crearProducto("ProdIso-" + System.nanoTime(), "12.00");
        String codigo = "CLI-ISO-" + System.nanoTime();
        String idem = "idem-iso-" + System.nanoTime();

        String body = """
                {
                  "codigo":"%s",
                  "metodoPago":"EFECTIVO",
                  "metodoEntrega":"RETIRAR",
                  "idempotencyKey":"%s",
                  "items":[{"productoId":%d,"cantidad":1,"extraIds":[],"ingredientesRemovidos":[]}]
                }
                """.formatted(codigo, idem, productoId);
        mockMvc.perform(post("/api/v1/clientes/pedidos")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        // B conoce el código y la clave válida: aun así no puede confirmar -> 404
        mockMvc.perform(post("/api/v1/clientes/pedidos/" + codigo + "/confirmar")
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idempotencyKey":"%s"}
                                """.formatted(idem)))
                .andExpect(status().isNotFound());

        // Clave distinta tampoco -> 404
        mockMvc.perform(post("/api/v1/clientes/pedidos/" + codigo + "/confirmar")
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idempotencyKey":"otra-%d"}
                                """.formatted(System.nanoTime())))
                .andExpect(status().isNotFound());

        // El propietario conserva la idempotencia: misma clave 200
        mockMvc.perform(post("/api/v1/clientes/pedidos/" + codigo + "/confirmar")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idempotencyKey":"%s"}
                                """.formatted(idem)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CONFIRMADO"));
    }

    @Test
    void respuestasNoExponenClavesNiIdentificadores() throws Exception {
        long productoId = crearProducto("ProdSig-" + System.nanoTime(), "6.50");
        String codigo = "CLI-SIG-" + System.nanoTime();
        String idem = "idem-sig-" + System.nanoTime();

        String body = """
                {
                  "codigo":"%s",
                  "metodoPago":"EFECTIVO",
                  "metodoEntrega":"RETIRAR",
                  "idempotencyKey":"%s",
                  "items":[{"productoId":%d,"cantidad":1,"extraIds":[],"ingredientesRemovidos":[]}]
                }
                """.formatted(codigo, idem, productoId);

        MvcResult creado = mockMvc.perform(post("/api/v1/clientes/pedidos")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();
        assertSinClaves(creado.getResponse().getContentAsString(), "crear");

        MvcResult carrito = mockMvc.perform(get("/api/v1/clientes/carrito")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode carritoJson = objectMapper.readTree(carrito.getResponse().getContentAsString());
        assertFalse(carritoJson.has("clienteId"), "carrito no debe exponer clienteId");

        MvcResult confirmado = mockMvc.perform(post("/api/v1/clientes/pedidos/" + codigo + "/confirmar")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idempotencyKey":"%s"}
                                """.formatted(idem)))
                .andExpect(status().isOk())
                .andReturn();
        assertSinClaves(confirmado.getResponse().getContentAsString(), "confirmar");

        MvcResult historial = mockMvc.perform(get("/api/v1/clientes/historial")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andReturn();
        String histJson = historial.getResponse().getContentAsString();
        assertTrue(histJson.contains(codigo), "historial incluye el pedido propio");
        assertSinClaves(histJson, "historial");
    }

    @Test
    void clienteBNoVePedidoDeClienteAEnHistorialNiCarrito() throws Exception {
        long productoId = crearProducto("ProdHid-" + System.nanoTime(), "8.00");
        String codigo = "CLI-HID-" + System.nanoTime();
        String idem = "idem-hid-" + System.nanoTime();

        String body = """
                {
                  "codigo":"%s",
                  "metodoPago":"TARJETA",
                  "metodoEntrega":"RETIRAR",
                  "idempotencyKey":"%s",
                  "items":[{"productoId":%d,"cantidad":1,"extraIds":[],"ingredientesRemovidos":[]}]
                }
                """.formatted(codigo, idem, productoId);
        mockMvc.perform(post("/api/v1/clientes/pedidos")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        MvcResult histB = mockMvc.perform(get("/api/v1/clientes/historial")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andReturn();
        assertFalse(histB.getResponse().getContentAsString().contains(codigo),
                "historial de B no debe contener el pedido de A");

        MvcResult carritoB = mockMvc.perform(get("/api/v1/clientes/carrito")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andReturn();
        assertFalse(carritoB.getResponse().getContentAsString().contains(codigo),
                "carrito de B no debe contener el pedido de A");
    }

    private void assertSinClaves(String json, String contexto) throws Exception {
        JsonNode node = objectMapper.readTree(json);
        if (node.isArray()) {
            for (JsonNode n : node) {
                assertFalse(n.has("idempotencyKey"), contexto + ": no idempotencyKey");
                assertFalse(n.has("clienteId"), contexto + ": no clienteId");
                assertFalse(n.has("direccionId"), contexto + ": no direccionId");
            }
        } else {
            assertFalse(node.has("idempotencyKey"), contexto + ": no idempotencyKey");
            assertFalse(node.has("clienteId"), contexto + ": no clienteId");
            assertFalse(node.has("direccionId"), contexto + ": no direccionId");
        }
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