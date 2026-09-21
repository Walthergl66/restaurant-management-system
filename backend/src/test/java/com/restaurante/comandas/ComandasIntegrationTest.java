package com.restaurante.comandas;

import com.restaurante.AbstractIntegracionApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Comandas: una por área al confirmar, órdenes de impresión idempotentes y
 * estados de cocina que avanzan al pedido.
 */
class ComandasIntegrationTest extends AbstractIntegracionApi {

    private String cocineroToken;
    private String meseroToken;
    private String administradorToken;

    @BeforeEach
    void preparar() {
        administradorToken = tokenAdmin();
        meseroToken = tokenMesero();
        cocineroToken = asegurarUsuario("cocina_test", "COCINA", "clave456");
    }

    @Test
    void confirmarGeneraUnaComandaPorAreaYOrdenDeImpresion() throws Exception {
        long areaCocina = crearArea("Cocina");
        long areaBarra = crearArea("Barra");
        long productoA = crearProductoConArea("Lomo Saltado", "11.00", areaCocina);
        long productoB = crearProductoConArea("Jugo Natural", "3.00", areaBarra);

        long mesaId = crearMesa();
        String codigo = "CMD-" + System.nanoTime();
        crearBorrador(mesaId, codigo);
        postItem(codigo, productoA, 2, null, null, null);
        postItem(codigo, productoA, 1, null, null, null);
        postItem(codigo, productoA, 1, null, "sin ají", null);
        postItem(codigo, productoB, 1, null, null, "con hielo");

        confirmar(codigo, "key-comandas-1");

        // Comandas: PENDIENTE (cocinero no las ha tocado) y líneas agregadas por producto
        JsonNode comandas = buscar(codigo, null);
        assertEquals(2, comandas.size(), "una comanda por área");
        JsonNode comandaCocina = buscarPorArea(comandas, areaCocina);
        assertEquals("PENDIENTE", comandaCocina.path("estado").asText());
        assertEquals(codigo, comandaCocina.path("pedidoCodigo").asText());
        JsonNode lineas = comandaCocina.path("lineas");
        assertEquals(2, lineas.size(), "1+2 sin variación se agrupan; la variación queda aparte");
        JsonNode agrupada = lineas.get(0);
        assertTrue(agrupada.path("nombreProducto").asText().startsWith("Lomo Saltado"));
        assertEquals(3, agrupada.path("cantidad").asInt(), "1 + 2 = 3");
        assertTrue(lineas.toString().contains("sin ají"));

        // Órdenes de impresión en el outbox
        assertEquals(2, buscarPendientes(codigo).size(), "una orden de impresión por comanda");
        JsonNode pendientes = buscarPendientes(codigo);
        assertTrue(pendientes.toString().contains("comanda.impresion"));
        assertTrue(pendientes.toString().contains("Barra"));
        assertTrue(pendientes.path(0).path("numeroComanda").isNumber());

        // El agente confirma la impresión: esa orden deja de entregarse
        long primera = pendientes.path(0).path("id").asLong();
        mockMvc.perform(post("/api/v1/comandas/impresion/" + primera + "/enviado")
                        .header("Authorization", "Bearer " + cocineroToken))
                .andExpect(status().isOk());
        assertEquals(1, buscarPendientes(codigo).size(), "la orden confirmada ya no es pendiente");
    }

    @Test
    void estadosDeCocinaAvanzanAlPedidoYLosEstadosSeFiltran() throws Exception {
        long area = crearArea("Parrilla");
        long producto = crearProductoConArea("Churrasco", "13.00", area);
        long mesaId = crearMesa();
        String codigo = "CMD-" + System.nanoTime();
        crearBorrador(mesaId, codigo);
        postItem(codigo, producto, 1, null, null, null);
        confirmar(codigo, "key-comandas-2");

        long comandaId = buscar(codigo, null).path(0).path("id").asLong();

        mockMvc.perform(post("/api/v1/comandas/" + comandaId + "/en-preparacion")
                        .header("Authorization", "Bearer " + cocineroToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("EN_PREPARACION"));

        // El pedido avanza en la misma operación
        mockMvc.perform(get("/api/v1/pedidos/" + codigo)
                        .header("Authorization", "Bearer " + meseroToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("EN_PREPARACION"));

        assertEquals(1, buscar(codigo, "EN_PREPARACION").size());
        assertEquals(0, buscar(codigo, "PENDIENTE").size());

        mockMvc.perform(post("/api/v1/comandas/" + comandaId + "/listo")
                        .header("Authorization", "Bearer " + cocineroToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("LISTO"));

        mockMvc.perform(get("/api/v1/pedidos/" + codigo)
                        .header("Authorization", "Bearer " + meseroToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("LISTO"));
    }

    @Test
    void confirmarIdempotenteNoDuplicaComandas() throws Exception {
        long area = crearArea("Horno");
        long producto = crearProductoConArea("Lasagna", "10.00", area);
        long mesaId = crearMesa();
        String codigo = "CMD-" + System.nanoTime();
        crearBorrador(mesaId, codigo);
        postItem(codigo, producto, 1, null, null, null);

        confirmar(codigo, "key-comandas-3");
        confirmar(codigo, "key-comandas-3");

        assertEquals(1, buscar(codigo, null).size(), "el reintento no duplica comandas");
        assertEquals(1, buscarPendientes(codigo).size(), "ni órdenes de impresión");
    }

    @Test
    void reimprimirReenvíaYElMeseroNoMarcaEnPreparacion() throws Exception {
        long area = crearArea("Cocina Veloz");
        long producto = crearProductoConArea("Burguer", "7.00", area);
        long mesaId = crearMesa();
        String codigo = "CMD-" + System.nanoTime();
        crearBorrador(mesaId, codigo);
        postItem(codigo, producto, 1, null, null, null);
        confirmar(codigo, "key-comandas-4");

        long comandaId = buscar(codigo, null).path(0).path("id").asLong();

        // Mesero no tiene permiso de marcar en preparación
        mockMvc.perform(post("/api/v1/comandas/" + comandaId + "/en-preparacion")
                        .header("Authorization", "Bearer " + meseroToken))
                .andExpect(status().isForbidden());

        // Reimpresión: se crea una nueva orden de impresión
        mockMvc.perform(post("/api/v1/comandas/" + comandaId + "/reimprimir")
                        .header("Authorization", "Bearer " + cocineroToken))
                .andExpect(status().isOk());

        assertEquals(2, buscarPendientes(codigo).size(), "1 original + 1 reimpresa");
    }

    @Test
    void productoSinAreaNoGeneraComanda() throws Exception {
        long producto = crearProductoSinArea("Agua", "1.00");
        long mesaId = crearMesa();
        String codigo = "CMD-" + System.nanoTime();
        crearBorrador(mesaId, codigo);
        postItem(codigo, producto, 1, null, null, null);
        confirmar(codigo, "key-comandas-5");

        assertEquals(0, buscar(codigo, null).size(), "un producto sin área no genera comanda");
    }

    // ---------------------------------------------------------------
    // Utilidades de contexto de datos
    // ---------------------------------------------------------------

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

    private JsonNode buscar(String codigo, String estado) throws Exception {
        JsonNode todas = listar(null, estado);
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

    private JsonNode listar(Long areaId, String estado) throws Exception {
        String url = "/api/v1/comandas";
        if (areaId != null || estado != null) {
            url += "?";
            if (areaId != null) {
                url += "areaId=" + areaId;
            }
            if (estado != null) {
                url += (areaId != null ? "&" : "") + "estado=" + estado;
            }
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

    private long crearMesa() throws Exception {
        String body = mockMvc.perform(post("/api/v1/mesas")
                        .header("Authorization", "Bearer " + administradorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"numero":%d,"capacidad":4}
                                """.formatted((int) (1000 + System.nanoTime() % 5000))))
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

    private void crearBorrador(long mesaId, String codigo) throws Exception {
        mockMvc.perform(post("/api/v1/pedidos")
                        .header("Authorization", "Bearer " + meseroToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"codigo":"%s","mesaId":%d}
                                """.formatted(codigo, mesaId)))
                .andExpect(status().isCreated());
    }

    private void postItem(String codigo, long productoId, int cantidad, Long extraId,
                          String ingrediente, String observaciones) throws Exception {
        String extras = extraId == null ? "[]" : "[" + extraId + "]";
        String removidos = ingrediente == null ? "[]" : "[\"" + ingrediente + "\"]";
        mockMvc.perform(post("/api/v1/pedidos/" + codigo + "/items")
                        .header("Authorization", "Bearer " + meseroToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productoId":%d,"cantidad":%d,"extraIds":%s,"ingredientesRemovidos":%s,"observaciones":"%s"}
                                """.formatted(productoId, cantidad, extras, removidos, observaciones == null ? "" : observaciones)))
                .andExpect(status().isCreated());
    }

    private void confirmar(String codigo, String clave) throws Exception {
        mockMvc.perform(post("/api/v1/pedidos/" + codigo + "/confirmar")
                        .header("Authorization", "Bearer " + meseroToken)
                        .header("Idempotency-Key", clave))
                .andExpect(status().isOk());
    }
}