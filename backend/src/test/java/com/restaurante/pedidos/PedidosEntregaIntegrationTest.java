package com.restaurante.pedidos;

import com.restaurante.AbstractIntegracionApi;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import tools.jackson.databind.JsonNode;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Entrega en salón: el mesero marca servido un pedido que cocina dejó LISTO. Incluye lo que concede la
 * migración V22 (el mesero y el rol COCINA operan las comandas) y el filtro del listado por estado.
 */
class PedidosEntregaIntegrationTest extends AbstractIntegracionApi {

    private static final AtomicInteger NUMERO_MESA = new AtomicInteger(32000);

    @Test
    void elMeseroEntregaUnPedidoListoYReintentarNoCambiaNada() throws Exception {
        String mesero = tokenMesero();
        String codigo = pedidoConfirmado(mesero);
        marcarComandaDelPedido(mesero, codigo, "en-preparacion");
        marcarComandaDelPedido(mesero, codigo, "listo");

        mockMvc.perform(post("/api/v1/pedidos/" + codigo + "/entregar").header("Authorization", "Bearer " + mesero))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ENTREGADO"));
        mockMvc.perform(post("/api/v1/pedidos/" + codigo + "/entregar").header("Authorization", "Bearer " + mesero))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ENTREGADO"));
    }

    @Test
    void entregarUnPedidoQueNoEstaListoSeRechaza() throws Exception {
        String mesero = tokenMesero();
        String codigo = pedidoConfirmado(mesero);

        mockMvc.perform(post("/api/v1/pedidos/" + codigo + "/entregar").header("Authorization", "Bearer " + mesero))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void sinElPermisoDeEntregarSeRespondeProhibido() throws Exception {
        String sinPermiso = tokenMeseroConPermisos("sin_entregar_" + System.nanoTime() % 1_000_000_000L, "pedidos:ver");

        mockMvc.perform(post("/api/v1/pedidos/CUALQUIERA/entregar").header("Authorization", "Bearer " + sinPermiso))
                .andExpect(status().isForbidden());
    }

    @Test
    void elListadoPuedeFiltrarseSoloPorEstado() throws Exception {
        String mesero = tokenMesero();
        long mesaId = crearMesa();
        String listo = pedidoConfirmadoEnMesa(mesero, mesaId);
        marcarComandaDelPedido(mesero, listo, "en-preparacion");
        marcarComandaDelPedido(mesero, listo, "listo");
        String soloConfirmado = pedidoConfirmadoEnMesa(mesero, mesaId);

        String cuerpo = mockMvc.perform(get("/api/v1/pedidos?estado=LISTO&mesaId=" + mesaId)
                        .header("Authorization", "Bearer " + mesero))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode pedidos = objectMapper.readTree(cuerpo);
        assertEquals(1, pedidos.size());
        assertEquals(listo, pedidos.get(0).path("codigo").asText());
        assertFalse(cuerpo.contains(soloConfirmado));
    }

    @Test
    void elRolCocinaOperaLasComandasPeroNoEntregaEnSalon() throws Exception {
        String mesero = tokenMesero();
        String codigo = pedidoConfirmado(mesero);
        String cocina = tokenCocina("cocina_rol_" + System.nanoTime() % 1_000_000_000L);

        long comandaId = comandaDelPedido(cocina, codigo);
        mockMvc.perform(post("/api/v1/comandas/" + comandaId + "/en-preparacion").header("Authorization", "Bearer " + cocina))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/comandas/" + comandaId + "/listo").header("Authorization", "Bearer " + cocina))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/pedidos/" + codigo + "/entregar").header("Authorization", "Bearer " + cocina))
                .andExpect(status().isForbidden());
    }

    // ---- helpers ----

    private String tokenCocina(String username) throws Exception {
        mockMvc.perform(post("/api/v1/usuarios")
                        .header("Authorization", "Bearer " + tokenAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","nombre":"Cocina Rol","password":"clave456","rolCodigo":"COCINA"}
                                """.formatted(username)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rol").value("COCINA"));
        return loginToken(username, "clave456");
    }

    private String pedidoConfirmado(String mesero) throws Exception {
        return pedidoConfirmadoEnMesa(mesero, crearMesa());
    }

    private String pedidoConfirmadoEnMesa(String mesero, long mesaId) throws Exception {
        String admin = tokenAdmin();
        long area = id(mockMvc.perform(post("/api/v1/areas").header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Entrega-%d\",\"descripcion\":\"prueba\"}".formatted(System.nanoTime() % 1_000_000)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
        long producto = id(mockMvc.perform(post("/api/v1/productos").header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Plato-%d\",\"precio\":6.00,\"areaId\":%d,\"extraIds\":[],\"ingredientes\":[]}"
                                .formatted(System.nanoTime() % 1_000_000, area)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());

        String codigo = "ENT-" + System.nanoTime();
        mockMvc.perform(post("/api/v1/pedidos").header("Authorization", "Bearer " + mesero)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"%s\",\"mesaId\":%d}".formatted(codigo, mesaId)))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/pedidos/" + codigo + "/items").header("Authorization", "Bearer " + mesero)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productoId\":%d,\"cantidad\":1,\"extraIds\":[],\"ingredientesRemovidos\":[]}".formatted(producto)))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/pedidos/" + codigo + "/confirmar").header("Authorization", "Bearer " + mesero)
                        .header("Idempotency-Key", "key-" + codigo))
                .andExpect(status().isOk());
        return codigo;
    }

    private long comandaDelPedido(String token, String pedidoCodigo) throws Exception {
        JsonNode todas = objectMapper.readTree(mockMvc.perform(get("/api/v1/comandas").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        for (JsonNode c : todas) {
            if (pedidoCodigo.equals(c.path("pedidoCodigo").asText()) && "ORDEN".equals(c.path("tipo").asText())) {
                return c.path("id").asLong();
            }
        }
        throw new AssertionError("El pedido " + pedidoCodigo + " no tiene comanda");
    }

    private void marcarComandaDelPedido(String token, String pedidoCodigo, String transicion) throws Exception {
        long id = comandaDelPedido(token, pedidoCodigo);
        mockMvc.perform(post("/api/v1/comandas/" + id + "/" + transicion).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        assertTrue(id > 0);
    }

    private long crearMesa() throws Exception {
        String cuerpo = mockMvc.perform(post("/api/v1/mesas").header("Authorization", "Bearer " + tokenAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"numero\":%d,\"capacidad\":4}".formatted(NUMERO_MESA.incrementAndGet())))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return id(cuerpo);
    }

    private long id(String json) throws Exception {
        return objectMapper.readTree(json).path("id").asLong();
    }
}
