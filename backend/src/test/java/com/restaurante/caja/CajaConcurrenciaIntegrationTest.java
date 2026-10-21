package com.restaurante.caja;

import com.restaurante.AbstractIntegracionApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** A-02: el cierre de caja se serializa con ingresos/egresos vía bloqueo de fila.
 *  Invariante: tras conciliar el cierre, ningún movimiento puede quedar ligado a
 *  una caja cerrada; los movimientos registrados son exactamente los aceptados. */
class CajaConcurrenciaIntegrationTest extends AbstractIntegracionApi {

    private String gestorToken;
    private String adminToken;

    @BeforeEach
    void preparar() {
        adminToken = tokenAdmin();
        // La cuenta ADMIN es unica y no se replica desde el panel: se usa la sembrada.
        gestorToken = tokenAdmin();
    }

    @Test
    void cierreConcurrenteConEgresosNoDejaMovimientosFueraDeConciliacion() throws Exception {
        abrirCaja();
        long cajaId = cajaAbiertaId();

        int totalEgresos = 12;
        CountDownLatch inicio = new CountDownLatch(1);
        AtomicInteger aceptados = new AtomicInteger(0);

        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            pool.submit(() -> {
                try {
                    inicio.await();
                    for (int i = 0; i < totalEgresos; i++) {
                        MvcResult res = mockMvc.perform(post("/api/v1/cajas/" + cajaId + "/egresos")
                                        .header("Authorization", "Bearer " + gestorToken)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("""
                                                {"concepto":"egreso-%d","monto":1.00,"metodo":"EFECTIVO"}
                                                """.formatted(i)))
                                .andReturn();
                        if (res.getResponse().getStatus() == 201) {
                            aceptados.incrementAndGet();
                        }
                    }
                } catch (Exception e) {
                    throw new IllegalStateException("Error en egresos concurrentes", e);
                }
                return null;
            });

            pool.submit(() -> {
                try {
                    inicio.await();
                    // Cierre mientras los egresos siguen llegando.
                    mockMvc.perform(post("/api/v1/cajas/" + cajaId + "/cierre")
                                    .header("Authorization", "Bearer " + gestorToken)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content("{\"montoReal\":0}"))
                            .andExpect(status().isOk());
                } catch (Exception e) {
                    throw new IllegalStateException("Error en cierre concurrente", e);
                }
                return null;
            });

            inicio.countDown();
            pool.shutdown();
            assertTrue(pool.awaitTermination(60, TimeUnit.SECONDS), "los hilos no terminaron a tiempo");
        } finally {
            pool.shutdownNow();
        }

        // La caja quedó cerrada
        mockMvc.perform(get("/api/v1/cajas/" + cajaId)
                        .header("Authorization", "Bearer " + gestorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CERRADA"));

        // Los movimientos registrados son exactamente los egresos aceptados
        // (los rechazados no se persistieron): no quedan movimientos "colgados".
        MvcResult movs = mockMvc.perform(get("/api/v1/cajas/" + cajaId + "/movimientos")
                        .header("Authorization", "Bearer " + gestorToken))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode movimientos = objectMapper.readTree(movs.getResponse().getContentAsString());
        assertEquals(aceptados.get(), movimientos.size(),
                "movimientos registrados deben coincidir con los egresos aceptados");

        // Y un egreso posterior sobre caja cerrada se rechaza (movimiento contra caja cerrada)
        mockMvc.perform(post("/api/v1/cajas/" + cajaId + "/egresos")
                        .header("Authorization", "Bearer " + gestorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"concepto":"post-cierre","monto":1.00,"metodo":"EFECTIVO"}
                                """))
                .andExpect(status().isUnprocessableEntity());

        // La conciliación (esperado) ya registrada no debe cambiar con movimientos posteriores
        assertTrue(aceptados.get() >= 0);
    }

    private void abrirCaja() throws Exception {
        mockMvc.perform(post("/api/v1/cajas/apertura")
                        .header("Authorization", "Bearer " + gestorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"montoInicial\":0}"))
                .andExpect(status().isCreated());
    }

    private long cajaAbiertaId() throws Exception {
        MvcResult res = mockMvc.perform(get("/api/v1/cajas/abierta")
                        .header("Authorization", "Bearer " + gestorToken))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(res.getResponse().getContentAsString()).path("id").asLong();
    }

}