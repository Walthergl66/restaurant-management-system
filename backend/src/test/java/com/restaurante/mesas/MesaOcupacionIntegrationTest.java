package com.restaurante.mesas;

import com.restaurante.AbstractIntegracionApi;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import tools.jackson.databind.JsonNode;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** La mesa queda OCUPADA mientras tenga una cuenta abierta y vuelve a LIBRE al cerrarse por cualquier camino. */
class MesaOcupacionIntegrationTest extends AbstractIntegracionApi {

    private static final AtomicInteger NUMERO_MESA = new AtomicInteger(33000);

    private String admin;
    private String mesero;

    @BeforeEach
    void preparar() {
        admin = tokenAdmin();
        mesero = tokenMesero();
    }

    /** La caja es un recurso único compartido entre pruebas: cada prueba deja su turno cerrado. */
    @AfterEach
    void limpiarCaja() throws Exception {
        var respuesta = mockMvc.perform(get("/api/v1/cajas/abierta").header("Authorization", "Bearer " + admin)).andReturn();
        if (respuesta.getResponse().getStatus() == 200) {
            long id = objectMapper.readTree(respuesta.getResponse().getContentAsString()).path("id").asLong();
            mockMvc.perform(post("/api/v1/cajas/" + id + "/cierre")
                    .header("Authorization", "Bearer " + admin)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"montoReal\":0}"));
        }
    }

    @Test
    void unaMesaNuevaEstaLibre() throws Exception {
        long mesa = crearMesa();
        assertEquals("LIBRE", estadoDe(mesa));
    }

    @Test
    void elPrimerPedidoOcupaLaMesaYUnSegundoPedidoLaMantieneOcupada() throws Exception {
        long mesa = crearMesa();
        crearBorrador(mesa);
        assertEquals("OCUPADA", estadoDe(mesa));

        crearBorrador(mesa);
        assertEquals("OCUPADA", estadoDe(mesa));
    }

    @Test
    void cobrarLaCuentaLiberaLaMesa() throws Exception {
        long mesa = crearMesa();
        String codigo = crearBorrador(mesa);
        long producto = crearProducto();
        agregarLineaYConfirmar(codigo, producto);
        long cuentaId = cuentaAbiertaDe(mesa);
        double total = objectMapper.readTree(mockMvc.perform(get("/api/v1/cuentas/" + cuentaId)
                        .header("Authorization", "Bearer " + admin))
                .andReturn().getResponse().getContentAsString()).path("total").asDouble();
        mockMvc.perform(post("/api/v1/cajas/apertura").header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"montoInicial\":10.00}"))
                .andExpect(status().isCreated());
        assertEquals("OCUPADA", estadoDe(mesa));

        mockMvc.perform(post("/api/v1/cuentas/" + cuentaId + "/cobros").header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pagos\":[{\"metodo\":\"EFECTIVO\",\"monto\":%s}]}".formatted(total)))
                .andExpect(status().isCreated());

        assertEquals("LIBRE", estadoDe(mesa));
    }

    @Test
    void cerrarLaCuentaAManoLiberaLaMesa() throws Exception {
        long mesa = crearMesa();
        String codigo = crearBorrador(mesa);
        agregarLineaYConfirmar(codigo, crearProducto());
        long cuentaId = cuentaAbiertaDe(mesa);
        assertEquals("OCUPADA", estadoDe(mesa));

        mockMvc.perform(patch("/api/v1/cuentas/" + cuentaId + "/cerrar").header("Authorization", "Bearer " + mesero))
                .andExpect(status().isOk());

        assertEquals("LIBRE", estadoDe(mesa));
    }

    @Test
    void cancelarElUnicoBorradorLiberaLaMesa() throws Exception {
        long mesa = crearMesa();
        String codigo = crearBorrador(mesa);
        assertEquals("OCUPADA", estadoDe(mesa));

        mockMvc.perform(delete("/api/v1/pedidos/" + codigo).header("Authorization", "Bearer " + mesero))
                .andExpect(status().isNoContent());

        assertEquals("LIBRE", estadoDe(mesa));
    }

    @Test
    void liberarUnaMesaNoAfectaALasDemas() throws Exception {
        long mesaA = crearMesa();
        long mesaB = crearMesa();
        String borradorA = crearBorrador(mesaA);
        crearBorrador(mesaB);

        mockMvc.perform(delete("/api/v1/pedidos/" + borradorA).header("Authorization", "Bearer " + mesero))
                .andExpect(status().isNoContent());

        assertEquals("LIBRE", estadoDe(mesaA));
        assertEquals("OCUPADA", estadoDe(mesaB));
    }

    @Test
    void deshabilitarUnaMesaOcupadaNoCambiaSuEstado() throws Exception {
        long mesa = crearMesa();
        crearBorrador(mesa);

        mockMvc.perform(patch("/api/v1/mesas/" + mesa + "/habilitacion").header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"habilitada\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("OCUPADA"));
    }

    // ---- helpers ----

    private String estadoDe(long mesaId) throws Exception {
        return objectMapper.readTree(mockMvc.perform(get("/api/v1/mesas/" + mesaId).header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).path("estado").asText();
    }

    private long crearMesa() throws Exception {
        String cuerpo = mockMvc.perform(post("/api/v1/mesas").header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"numero\":%d,\"capacidad\":4}".formatted(NUMERO_MESA.incrementAndGet())))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(cuerpo).path("id").asLong();
    }

    private String crearBorrador(long mesaId) throws Exception {
        String codigo = "OCU-" + System.nanoTime();
        mockMvc.perform(post("/api/v1/pedidos").header("Authorization", "Bearer " + mesero)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"%s\",\"mesaId\":%d}".formatted(codigo, mesaId)))
                .andExpect(status().isCreated());
        return codigo;
    }

    private long crearProducto() throws Exception {
        String cuerpo = mockMvc.perform(post("/api/v1/productos").header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Plato-%d\",\"precio\":8.00,\"extraIds\":[],\"ingredientes\":[]}"
                                .formatted(System.nanoTime() % 1_000_000)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(cuerpo).path("id").asLong();
    }

    private void agregarLineaYConfirmar(String codigo, long productoId) throws Exception {
        mockMvc.perform(post("/api/v1/pedidos/" + codigo + "/items").header("Authorization", "Bearer " + mesero)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productoId\":%d,\"cantidad\":1,\"extraIds\":[],\"ingredientesRemovidos\":[]}".formatted(productoId)))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/pedidos/" + codigo + "/confirmar").header("Authorization", "Bearer " + mesero)
                        .header("Idempotency-Key", "key-" + codigo))
                .andExpect(status().isOk());
    }

    private long cuentaAbiertaDe(long mesaId) throws Exception {
        JsonNode cuentas = objectMapper.readTree(mockMvc.perform(get("/api/v1/cuentas?mesaId=" + mesaId)
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        for (JsonNode cuenta : cuentas) {
            if ("ABIERTA".equals(cuenta.path("estado").asText())) {
                return cuenta.path("id").asLong();
            }
        }
        throw new AssertionError("No hay cuenta abierta para la mesa " + mesaId);
    }
}
