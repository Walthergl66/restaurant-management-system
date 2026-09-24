package com.restaurante.clientes;

import com.restaurante.AbstractIntegracionApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Etapa 2.2: la contención por código único o clave de idempotencia nunca debe
 * acabar en 500; los reintentos/duplicados concurrentes terminan en 200 (reintento
 * idempotente) o 409 (conflicto), nunca en error interno.
 */
class PedidosClienteConcurrenciaIntegrationTest extends AbstractIntegracionApi {

    private String adminToken;
    private String clienteToken;

    @BeforeEach
    void preparar() {
        adminToken = tokenAdmin();
        clienteToken = asegurarCliente("cliente_conc", "claveCli123");
    }

    @Test
    void crearMismoCodigoConcurrenteNuncaDevuelve500() throws Exception {
        long productoId = crearProducto("ProdConc-" + System.nanoTime(), "5.00");
        String codigo = "CLI-CONC-" + System.nanoTime();
        String idem = "idem-conc-" + System.nanoTime();
        String body = pedidoJson(codigo, productoId, idem);

        int hilos = 6;
        CountDownLatch inicio = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(hilos);
        AtomicInteger ok = new AtomicInteger();
        AtomicInteger conflicto = new AtomicInteger();
        AtomicInteger error = new AtomicInteger();
        try {
            for (int i = 0; i < hilos; i++) {
                pool.submit(() -> {
                    try {
                        inicio.await();
                        int status = mockMvc.perform(post("/api/v1/clientes/pedidos")
                                        .header("Authorization", "Bearer " + clienteToken)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(body))
                                .andReturn().getResponse().getStatus();
                        if (status == 200) {
                            ok.incrementAndGet();
                        } else if (status == 409) {
                            conflicto.incrementAndGet();
                        } else {
                            error.incrementAndGet();
                        }
                    } catch (Exception e) {
                        throw new IllegalStateException("Error en crear concurrente", e);
                    }
                    return null;
                });
            }
            inicio.countDown();
            pool.shutdown();
            assertTrue(pool.awaitTermination(60, TimeUnit.SECONDS), "los hilos no terminaron a tiempo");
        } finally {
            pool.shutdownNow();
        }

        assertEquals(0, error.get(), "ninguna petición debe terminar en 500");
        assertTrue(ok.get() >= 1, "al menos una creación debe confirmar el pedido");
    }

    @Test
    void crearMismoCodigoClavesDistintasConcurrenteNuncaDevuelve500() throws Exception {
        long productoId = crearProducto("ProdConc2-" + System.nanoTime(), "5.00");
        String codigo = "CLI-CONC2-" + System.nanoTime();

        int hilos = 5;
        CountDownLatch inicio = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(hilos);
        AtomicInteger conflicto = new AtomicInteger();
        AtomicInteger error = new AtomicInteger();
        try {
            for (int i = 0; i < hilos; i++) {
                String idem = "idem-conc2-" + i + "-" + System.nanoTime();
                String body = pedidoJson(codigo, productoId, idem);
                pool.submit(() -> {
                    try {
                        inicio.await();
                        int status = mockMvc.perform(post("/api/v1/clientes/pedidos")
                                        .header("Authorization", "Bearer " + clienteToken)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(body))
                                .andReturn().getResponse().getStatus();
                        if (status == 409) {
                            conflicto.incrementAndGet();
                        } else if (status != 200) {
                            error.incrementAndGet();
                        }
                    } catch (Exception e) {
                        throw new IllegalStateException("Error en crear concurrente", e);
                    }
                    return null;
                });
            }
            inicio.countDown();
            pool.shutdown();
            assertTrue(pool.awaitTermination(60, TimeUnit.SECONDS), "los hilos no terminaron a tiempo");
        } finally {
            pool.shutdownNow();
        }

        assertEquals(0, error.get(), "ninguna petición debe terminar en 500");
        assertTrue(conflicto.get() >= 1, "las claves distintas deben caer en 409");
    }

    private String pedidoJson(String codigo, long productoId, String idem) {
        return """
                {
                  "codigo":"%s",
                  "metodoPago":"EFECTIVO",
                  "metodoEntrega":"RETIRAR",
                  "idempotencyKey":"%s",
                  "items":[{"productoId":%d,"cantidad":1,"extraIds":[],"ingredientesRemovidos":[]}]
                }
                """.formatted(codigo, idem, productoId);
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