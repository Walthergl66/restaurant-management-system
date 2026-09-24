package com.restaurante.comandas;

import com.restaurante.AbstractIntegracionApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;

import java.util.Arrays;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A-08: al confirmar un pedido de la app del cliente, un listener genera en la
 * MISMA transacción una comanda por área y su orden de impresión en el outbox,
 * visible en cocina con el código del cliente. Idempotente ante reintentos y
 * sin comanda cuando ningún producto tiene área.
 */
class ComandasClienteIntegrationTest extends AbstractIntegracionApi {

    private String administradorToken;
    private String cocineroToken;
    private String clienteToken;

    @BeforeEach
    void preparar() {
        administradorToken = tokenAdmin();
        cocineroToken = asegurarUsuario("cocina_clicom", "COCINA", "clave456");
        clienteToken = asegurarCliente("cliente_anticom", "claveAnt123");
    }

    @Test
    void confirmarPedidoClienteGeneraComandaPorAreaYOrdenDeImpresion() throws Exception {
        long areaCocina = crearArea("Cocina");
        long areaBarra = crearArea("Barra");
        long productoA = crearProductoConArea("Pollo Cli", "8.50", areaCocina);
        long productoB = crearProductoConArea("Jugo Cli", "2.50", areaBarra);

        String codigo = "CLI-COM-" + System.nanoTime();
        String idem = "idem-clicom-" + System.nanoTime();
        crearPedidoCliente(codigo, idem, productoA, productoB);
        confirmarCliente(codigo, idem);

        // Comandas: una por área y el código del cliente como identificador
        JsonNode comandas = buscar(codigo, null);
        assertEquals(2, comandas.size(), "una comanda por área");
        JsonNode cocina = buscarPorArea(comandas, areaCocina);
        assertEquals("PENDIENTE", cocina.path("estado").asText());
        assertEquals(codigo, cocina.path("pedidoCodigo").asText());
        assertEquals(1, cocina.path("lineas").size());
        assertTrue(cocina.path("lineas").get(0).path("nombreProducto").asText().startsWith("Pollo Cli"));
        assertEquals(2, cocina.path("lineas").get(0).path("cantidad").asInt());

        // Órdenes de impresión en el outbox
        JsonNode pendientes = buscarPendientes(codigo);
        assertEquals(2, pendientes.size(), "una orden de impresión por comanda");
        assertTrue(pendientes.toString().contains("comanda.impresion"));
        assertTrue(pendientes.toString().contains("Barra"));
    }

    @Test
    void confirmarIdempotenteNoDuplicaComandasNiOrdenes() throws Exception {
        long area = crearArea("Horno");
        long producto = crearProductoConArea("Lasagna Cli", "9.00", area);
        String codigo = "CLI-COM-" + System.nanoTime();
        String idem = "idem-clicom-" + System.nanoTime();
        crearPedidoCliente(codigo, idem, producto);
        confirmarCliente(codigo, idem);
        confirmarCliente(codigo, idem);

        assertEquals(1, buscar(codigo, null).size(), "el reintento no duplica comandas");
        assertEquals(1, buscarPendientes(codigo).size(), "ni órdenes de impresión");
    }

    @Test
    void productoSinAreaNoGeneraComanda() throws Exception {
        long producto = crearProductoSinArea("Agua Cli", "1.00");
        String codigo = "CLI-COM-" + System.nanoTime();
        String idem = "idem-clicom-" + System.nanoTime();
        crearPedidoCliente(codigo, idem, producto);
        confirmarCliente(codigo, idem);

        assertEquals(0, buscar(codigo, null).size(), "un producto sin área no genera comanda");
    }

    @Test
    void cocinaAvanzaPedidoClienteDesdeLaComanda() throws Exception {
        long area = crearArea("Parrilla");
        long producto = crearProductoConArea("Churrasco Cli", "14.00", area);
        String codigo = "CLI-COM-" + System.nanoTime();
        String idem = "idem-clicom-" + System.nanoTime();
        crearPedidoCliente(codigo, idem, producto);
        confirmarCliente(codigo, idem);

        long comandaId = buscar(codigo, null).path(0).path("id").asLong();
        mockMvc.perform(post("/api/v1/comandas/" + comandaId + "/en-preparacion")
                        .header("Authorization", "Bearer " + cocineroToken))
                .andExpect(status().isOk());

        // La comanda y el pedido del cliente avanzan en la misma operación
        JsonNode comandas = buscar(codigo, "EN_PREPARACION");
        assertEquals(1, comandas.size());

        JsonNode historial = objectMapper.readTree(mockMvc.perform(get("/api/v1/clientes/historial?page=0&size=50")
                        .header("Authorization", "Bearer " + clienteToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        JsonNode pedido = null;
        for (JsonNode n : historial.path("content")) {
            if (codigo.equals(n.path("codigo").asText())) {
                pedido = n;
            }
        }
        assertEquals("EN_PREPARACION", pedido.path("estado").asText());
    }

    // ---------------------------------------------------------------
    // Utilidades de contexto de datos
    // ---------------------------------------------------------------

    private void crearPedidoCliente(String codigo, String idem, long... productoIds) throws Exception {
        String items = Arrays.stream(productoIds)
                .mapToObj(p -> "{\"productoId\":%d,\"cantidad\":2,\"extraIds\":[],\"ingredientesRemovidos\":[]}"
                        .formatted(p))
                .collect(Collectors.joining(","));
        mockMvc.perform(post("/api/v1/clientes/pedidos")
                        .header("Authorization", "Bearer " + clienteToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "codigo":"%s",
                                  "metodoPago":"EFECTIVO",
                                  "metodoEntrega":"RETIRAR",
                                  "idempotencyKey":"%s",
                                  "items":[%s]
                                }
                                """.formatted(codigo, idem, items)))
                .andExpect(status().isOk());
    }

    private void confirmarCliente(String codigo, String idem) throws Exception {
        mockMvc.perform(post("/api/v1/clientes/pedidos/" + codigo + "/confirmar")
                        .header("Authorization", "Bearer " + clienteToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idempotencyKey":"%s"}
                                """.formatted(idem)))
                .andExpect(status().isOk());
    }

    private String asegurarUsuario(String username, String rol, String password) {
        try {
            mockMvc.perform(post("/api/v1/usuarios")
                            .header("Authorization", "Bearer " + administradorToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"username":"%s","nombre":"%s","password":"%s","rolCodigo":"%s"}
                                    """.formatted(username, username, password, rol)))
                    .andExpect(result -> {
                        int status = result.getResponse().getStatus();
                        if (status != 201 && status != 409) {
                            throw new AssertionError("Esperaba 201 o 409, fue " + status);
                        }
                    });
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo asegurar el usuario " + username, e);
        }
        return loginToken(username, password);
    }

    private String asegurarCliente(String username, String password) {
        try {
            mockMvc.perform(post("/api/v1/usuarios")
                            .header("Authorization", "Bearer " + administradorToken)
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
            throw new IllegalStateException("No se pudo asegurar el cliente " + username, e);
        }
        return loginToken(username, password);
    }

    private JsonNode buscar(String codigo, String estado) throws Exception {
        JsonNode todas = listar(estado);
        var result = new java.util.ArrayList<JsonNode>();
        for (JsonNode c : todas) {
            if (codigo.equals(c.path("pedidoCodigo").asText())) {
                result.add(c);
            }
        }
        return objectMapper.readTree(objectMapper.writeValueAsString(result));
    }

    private JsonNode buscarPendientes(String codigo) throws Exception {
        JsonNode todas = impresionPendientes();
        var result = new java.util.ArrayList<JsonNode>();
        for (JsonNode c : todas) {
            if (codigo.equals(c.path("pedidoCodigo").asText())) {
                result.add(c);
            }
        }
        return objectMapper.readTree(objectMapper.writeValueAsString(result));
    }

    private JsonNode listar(String estado) throws Exception {
        String url = "/api/v1/comandas";
        if (estado != null) {
            url += "?estado=" + estado;
        }
        MvcResult res = mockMvc.perform(get(url)
                        .header("Authorization", "Bearer " + cocineroToken))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(res.getResponse().getContentAsString());
    }

    private JsonNode impresionPendientes() throws Exception {
        MvcResult res = mockMvc.perform(get("/api/v1/comandas/impresion/pendientes")
                        .header("Authorization", "Bearer " + cocineroToken))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(res.getResponse().getContentAsString());
    }

    private JsonNode buscarPorArea(JsonNode comandas, long areaId) {
        for (JsonNode c : comandas) {
            if (c.path("areaId").asLong() == areaId) {
                return c;
            }
        }
        throw new AssertionError("No hay comanda para el área " + areaId);
    }

    private long crearArea(String nombre) throws Exception {
        String body = mockMvc.perform(post("/api/v1/areas")
                        .header("Authorization", "Bearer " + administradorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"%s-%d","descripcion":"área de prueba"}
                                """.formatted(nombre, System.nanoTime() % 100000)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).path("id").asLong();
    }

    private long crearProductoConArea(String nombre, String precio, long areaId) throws Exception {
        String body = mockMvc.perform(post("/api/v1/productos")
                        .header("Authorization", "Bearer " + administradorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"%s-%d","precio":%s,"areaId":%d,"extraIds":[],"ingredientes":[]}
                                """.formatted(nombre, System.nanoTime() % 100000, precio, areaId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).path("id").asLong();
    }

    private long crearProductoSinArea(String nombre, String precio) throws Exception {
        String body = mockMvc.perform(post("/api/v1/productos")
                        .header("Authorization", "Bearer " + administradorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"%s-%d","precio":%s,"extraIds":[],"ingredientes":[]}
                                """.formatted(nombre, System.nanoTime() % 100000, precio)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).path("id").asLong();
    }
}