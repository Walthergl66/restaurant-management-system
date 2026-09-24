package com.restaurante.usuarios;

import com.restaurante.AbstractIntegracionApi;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** A-03: el refresh token es de un solo uso. La rotación es atómica (bloqueo
 *  pesimista): de dos rotaciones simultáneas del mismo token solo una gana; la
 *  reutilización posterior revoca la familia de sesiones del usuario. */
class AuthRotacionRefreshTest extends AbstractIntegracionApi {

    @Test
    void refreshTokenEsDeUnSoloUsoYReusoRevocaFamilia() throws Exception {
        String refresh = loginYRefresh();

        // Primer uso: rotación válida
        MvcResult primero = mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"%s"}
                                """.formatted(refresh)))
                .andReturn();
        assertEquals(200, primero.getResponse().getStatus(), "primera rotación debe ser 200");

        String nuevoRefresh = objectMapper.readTree(primero.getResponse().getContentAsString())
                .path("refreshToken").asText();
        assertTrue(nuevoRefresh.length() > 0);

        // Reutilizar el token ya consumido: 401 y matanza de la familia, de modo
        // que incluso el token hijo ya emitido queda revocado.
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"%s"}
                                """.formatted(refresh)))
                .andReturn();
        MvcResult hijo = mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"%s"}
                                """.formatted(nuevoRefresh)))
                .andReturn();
        assertEquals(401, hijo.getResponse().getStatus(),
                "el token hijo queda revocado tras detectar reutilización del padre");
    }

    @Test
    void rotacionConcurrenteSoloUnaGana() throws Exception {
        String refresh = loginYRefresh();
        int intentos = 8;
        CountDownLatch inicio = new CountDownLatch(1);
        AtomicInteger exitos = new AtomicInteger(0);

        ExecutorService pool = Executors.newFixedThreadPool(intentos);
        try {
            for (int i = 0; i < intentos; i++) {
                pool.submit(() -> {
                    try {
                        inicio.await();
                        MvcResult res = mockMvc.perform(post("/api/v1/auth/refresh")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("""
                                                {"refreshToken":"%s"}
                                                """.formatted(refresh)))
                                .andReturn();
                        if (res.getResponse().getStatus() == 200) {
                            exitos.incrementAndGet();
                        }
                    } catch (Exception e) {
                        throw new IllegalStateException("Error en refresh concurrente", e);
                    }
                    return null;
                });
            }

            inicio.countDown();
            pool.shutdown();
            assertTrue(pool.awaitTermination(60, TimeUnit.SECONDS), "los hilos no terminaron");
        } finally {
            pool.shutdownNow();
        }

        assertEquals(1, exitos.get(), "de N rotaciones simultáneas del mismo token solo una gana");
    }

    private String loginYRefresh() throws Exception {
        MvcResult login = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"admin","password":"admin123"}
                                """))
                .andReturn();
        JsonNode body = objectMapper.readTree(login.getResponse().getContentAsString());
        return body.path("refreshToken").asText();
    }
}