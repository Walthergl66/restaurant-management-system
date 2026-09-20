package com.restaurante.pedidos;

import com.restaurante.AbstractIntegracionApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Flujo de pedidos presenciales: borrador, personalización, resumen y
 * confirmación con llave de idempotencia.
 */
class PedidosIntegrationTest extends AbstractIntegracionApi {

    private String meseroToken;
    private String administradorToken;

    @BeforeEach
    void preparar() {
        administradorToken = tokenAdmin();
        meseroToken = tokenMesero();
    }

    @Test
    void creaBorradorOcupaMesaPersonalizaYResume() throws Exception {
        long mesaId = crearMesa();
        long productoId = crearProducto("Pollo a la Plancha", "12.50");
        long extraId = crearExtra("Extra Queso", "1.50");

        String codigo = crearBorrador(mesaId);

        postItem(codigo, productoId, 2, extraId, "sin sal", "poco hecha");

        MvcResult resumen = getPedido(codigo);
        JsonNode lineas = objectMapper.readTree(resumen.getResponse().getContentAsString()).path("lineas");
        JsonNode linea = lineas.get(0);

        assertTrue(resumen.getResponse().getContentAsString().contains("\"estado\":\"BORRADOR\""));
        assertEquals("Pollo a la Plancha", linea.path("nombreProducto").asText());
        assertEquals(0, new BigDecimal("12.50").compareTo(linea.path("precioUnitario").decimalValue()));
        assertEquals(2, linea.path("cantidad").asInt());
        // (12.50 * 2) + (1.50 * 2)
        assertEquals(0, new BigDecimal("28.00").compareTo(linea.path("subtotal").decimalValue()));
        assertTrue(linea.path("ingredientesRemovidos").toString().contains("sin sal"));
        assertEquals(1, linea.path("extras").size());
        assertEquals("Extra Queso", linea.path("extras").get(0).path("nombre").asText());
        assertEquals(0, new BigDecimal("28.00").compareTo(
                objectMapper.readTree(resumen.getResponse().getContentAsString()).path("total").decimalValue()));

        String mesa = mockMvc.perform(get("/api/v1/mesas/" + mesaId)
                        .header("Authorization", "Bearer " + administradorToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertTrue(mesa.contains("\"estado\":\"OCUPADA\""), "La mesa debe quedar ocupada: " + mesa);

        mockMvc.perform(delete("/api/v1/pedidos/" + codigo)
                        .header("Authorization", "Bearer " + meseroToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void confirmarCongelaPreciosBloqueaCambiosYEsIdempotente() throws Exception {
        long mesaId = crearMesa();
        long productoId = crearProducto("Ceviche", "9.00");
        String codigo = crearBorrador(mesaId);

        postItem(codigo, productoId, 1, null, null, null);

        // Sin llave de idempotencia se rechaza
        mockMvc.perform(post("/api/v1/pedidos/" + codigo + "/confirmar")
                        .header("Authorization", "Bearer " + meseroToken))
                .andExpect(status().isUnprocessableEntity());

        mockMvc.perform(post("/api/v1/pedidos/" + codigo + "/confirmar")
                        .header("Authorization", "Bearer " + meseroToken)
                        .header("Idempotency-Key", "clave-confirm-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CONFIRMADO"));

        // Bloqueado: ni agregar ni quitar líneas
        mockMvc.perform(post("/api/v1/pedidos/" + codigo + "/items")
                        .header("Authorization", "Bearer " + meseroToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productoId":%d,"cantidad":1}
                                """.formatted(productoId)))
                .andExpect(status().isUnprocessableEntity());

        // Reintento con la MISMA llave: idempotente, mismo pedido
        mockMvc.perform(post("/api/v1/pedidos/" + codigo + "/confirmar")
                        .header("Authorization", "Bearer " + meseroToken)
                        .header("Idempotency-Key", "clave-confirm-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CONFIRMADO"));

        // Llave distinta sobre un pedido ya confirmado: conflicto
        mockMvc.perform(post("/api/v1/pedidos/" + codigo + "/confirmar")
                        .header("Authorization", "Bearer " + meseroToken)
                        .header("Idempotency-Key", "clave-confirm-2"))
                .andExpect(status().isConflict());

        // Cancelar un pedido confirmado no se permite
        mockMvc.perform(delete("/api/v1/pedidos/" + codigo)
                        .header("Authorization", "Bearer " + meseroToken))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void actualizarYQuitarLineaEnBorradorRecalculanTotal() throws Exception {
        long mesaId = crearMesa();
        long productoId = crearProducto("Arroz Marinero", "8.00");
        String codigo = crearBorrador(mesaId);

        MvcResult creada = postItemConRespuesta(codigo, productoId, 2, null);
        long lineaId = objectMapper.readTree(creada.getResponse().getContentAsString())
                .path("lineas").get(0).path("id").asLong();

        mockMvc.perform(put("/api/v1/pedidos/" + codigo + "/items/" + lineaId)
                        .header("Authorization", "Bearer " + meseroToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"cantidad":3,"extraIds":[],"ingredientesRemovidos":[]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(24.00));

        mockMvc.perform(delete("/api/v1/pedidos/" + codigo + "/items/" + lineaId)
                        .header("Authorization", "Bearer " + meseroToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lineas").isEmpty());
    }

    @Test
    void cancelarBorradorLiberaLaMesa() throws Exception {
        long mesaId = crearMesa();
        long productoId = crearProducto("Encebollado", "5.00");
        String codigo = crearBorrador(mesaId);
        postItem(codigo, productoId, 1, null, null, null);

        mockMvc.perform(delete("/api/v1/pedidos/" + codigo)
                        .header("Authorization", "Bearer " + meseroToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/pedidos/" + codigo)
                        .header("Authorization", "Bearer " + meseroToken))
                .andExpect(status().isNotFound());

        String mesa = mockMvc.perform(get("/api/v1/mesas/" + mesaId)
                        .header("Authorization", "Bearer " + administradorToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertTrue(mesa.contains("\"estado\":\"LIBRE\""), "La mesa debe quedar libre: " + mesa);
    }

    @Test
    void codigoDuplicadoYPermisos() throws Exception {
        long mesaId = crearMesa();
        String codigo = "PED-REPE-1";
        crearBorrador(mesaId, codigo);

        // Código de pedido duplicado → 409
        mockMvc.perform(post("/api/v1/pedidos")
                        .header("Authorization", "Bearer " + meseroToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"codigo":"%s","mesaId":%d}
                                """.formatted(codigo, mesaId)))
                .andExpect(status().isConflict());

        // Sin token → 401
        mockMvc.perform(post("/api/v1/pedidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"codigo":"PED-NO-TOKEN","mesaId":%d}
                                """.formatted(mesaId)))
                .andExpect(status().isUnauthorized());

        // Cocinero sin permiso de crear → 403
        String cocineroToken = asegurarUsuario("cocina_test", "COCINA", "clave456");
        mockMvc.perform(post("/api/v1/pedidos")
                        .header("Authorization", "Bearer " + cocineroToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"codigo":"PED-COCINA","mesaId":%d}
                                """.formatted(mesaId)))
                .andExpect(status().isForbidden());
    }

    @Test
    void noSeConfirmaPedidoVacio() throws Exception {
        long mesaId = crearMesa();
        String codigo = crearBorrador(mesaId);

        mockMvc.perform(post("/api/v1/pedidos/" + codigo + "/confirmar")
                        .header("Authorization", "Bearer " + meseroToken)
                        .header("Idempotency-Key", "clave-vacio"))
                .andExpect(status().isUnprocessableEntity());
    }

    // ---------------------------------------------------------------
    // Utilidades de contexto de datos
    // ---------------------------------------------------------------

    private String crearBorrador(long mesaId) throws Exception {
        return crearBorrador(mesaId, "PED-" + System.nanoTime());
    }

    private String crearBorrador(long mesaId, String codigo) throws Exception {
        mockMvc.perform(post("/api/v1/pedidos")
                        .header("Authorization", "Bearer " + meseroToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"codigo":"%s","mesaId":%d}
                                """.formatted(codigo, mesaId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("BORRADOR"));
        return codigo;
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
                .andExpect(status().isCreated())
                .andReturn();
    }

    private MvcResult postItemConRespuesta(String codigo, long productoId, int cantidad, Long extraId) throws Exception {
        String extras = extraId == null ? "[]" : "[" + extraId + "]";
        return mockMvc.perform(post("/api/v1/pedidos/" + codigo + "/items")
                        .header("Authorization", "Bearer " + meseroToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productoId":%d,"cantidad":%d,"extraIds":%s}
                                """.formatted(productoId, cantidad, extras)))
                .andExpect(status().isCreated())
                .andReturn();
    }

    private MvcResult getPedido(String codigo) throws Exception {
        return mockMvc.perform(get("/api/v1/pedidos/" + codigo)
                        .header("Authorization", "Bearer " + meseroToken))
                .andExpect(status().isOk())
                .andReturn();
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

    private long crearProducto(String nombre, String precio) throws Exception {
        String body = mockMvc.perform(post("/api/v1/productos")
                        .header("Authorization", "Bearer " + administradorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"%s-%d","precio":%s,"extraIds":[],"ingredientes":["cebolla"]}
                                """.formatted(nombre, System.nanoTime() % 100000, precio)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).path("id").asLong();
    }

    private long crearExtra(String nombre, String precio) throws Exception {
        String body = mockMvc.perform(post("/api/v1/extras")
                        .header("Authorization", "Bearer " + administradorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"%s","precio":%s}
                                """.formatted(nombre, precio)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).path("id").asLong();
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
}