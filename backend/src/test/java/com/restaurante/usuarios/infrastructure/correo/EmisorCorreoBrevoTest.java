package com.restaurante.usuarios.infrastructure.correo;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Adaptador de Brevo contra un servidor HTTP local: se verifica el contrato
 * (cabecera {@code api-key}, JSON, plantillas rellenas) y el manejo de fallos
 * sin depender de la red ni de credenciales reales.
 */
class EmisorCorreoBrevoTest {

    private static final String API_KEY = "clave-de-prueba-no-real";

    private HttpServer servidor;
    private final AtomicReference<String> apiKeyRecibida = new AtomicReference<>();
    private final AtomicReference<String> cuerpoRecibido = new AtomicReference<>();
    private volatile int estadoRespuesta;
    private volatile String cuerpoRespuesta;
    private volatile long retrasoMs;

    @BeforeEach
    void levantarServidor() throws IOException {
        estadoRespuesta = 201;
        cuerpoRespuesta = "{\"messageId\":\"<2026@brevo>\"}";
        retrasoMs = 0;
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/v3/smtp/email", intercambio -> {
            apiKeyRecibida.set(intercambio.getRequestHeaders().getFirst("api-key"));
            cuerpoRecibido.set(new String(intercambio.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            if (retrasoMs > 0) {
                try {
                    Thread.sleep(retrasoMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            byte[] respuesta = cuerpoRespuesta.getBytes(StandardCharsets.UTF_8);
            intercambio.getResponseHeaders().add("Content-Type", "application/json");
            intercambio.sendResponseHeaders(estadoRespuesta, respuesta.length);
            try (var salida = intercambio.getResponseBody()) {
                salida.write(respuesta);
            } catch (IOException ignorado) {
                // El cliente ya cerró por timeout: el test solo mira su excepción.
            }
        });
        servidor.start();
    }

    @AfterEach
    void apagarServidor() {
        servidor.stop(0);
    }

    @Test
    @DisplayName("envía el código de verificación con api-key, remitente y plantillas rellenas")
    void enviaCodigoDeVerificacion() throws Exception {
        emisor(Duration.ofSeconds(2)).enviarCodigoVerificacion("cliente@test.com", "482913");

        assertEquals(API_KEY, apiKeyRecibida.get());
        JsonNode peticion = JsonMapper.builder().build().readTree(cuerpoRecibido.get());
        assertEquals("no-reply@restaurante.test", peticion.path("sender").path("email").asText());
        assertEquals("Estación Burger", peticion.path("sender").path("name").asText());
        assertEquals("cliente@test.com", peticion.path("to").get(0).path("email").asText());
        assertTrue(peticion.path("subject").asText().contains("Verifica"));
        assertTrue(peticion.path("htmlContent").asText().contains("482913"));
        assertTrue(peticion.path("htmlContent").asText().contains("30 minutos"));
        assertTrue(peticion.path("textContent").asText().contains("482913"));
        assertTrue(peticion.path("tags").get(0).asText().contains("verificacion"));
    }

    @Test
    @DisplayName("envía la recuperación con el token y la vigencia recibida")
    void enviaRecuperacionDePassword() throws Exception {
        emisor(Duration.ofSeconds(2)).enviarRecuperacionPassword(
                "cliente@test.com", "token-de-un-solo-uso", Duration.ofMinutes(30));

        JsonNode peticion = JsonMapper.builder().build().readTree(cuerpoRecibido.get());
        assertTrue(peticion.path("subject").asText().contains("Recupera"));
        assertTrue(peticion.path("htmlContent").asText().contains("token-de-un-solo-uso"));
        assertTrue(peticion.path("htmlContent").asText().contains("30 minutos"));
        assertTrue(peticion.path("textContent").asText().contains("token-de-un-solo-uso"));
    }

    @Test
    @DisplayName("un 4xx de Brevo se convierte en excepción sin filtrar la api-key")
    void fallaConRespuestaNoExitosa() {
        estadoRespuesta = 400;
        cuerpoRespuesta = "{\"code\":\"invalid_parameter\",\"message\":\"sender not verified\"}";

        EnvioCorreoException error = assertThrows(EnvioCorreoException.class,
                () -> emisor(Duration.ofSeconds(2)).enviarCodigoVerificacion("cliente@test.com", "123456"));

        assertTrue(error.getMessage().contains("400"));
        assertTrue(error.getMessage().contains("sender not verified"));
        assertFalse(error.getMessage().contains(API_KEY));
    }

    @Test
    @DisplayName("un proveedor que no responde termina en excepción por timeout")
    void fallaPorTimeout() {
        retrasoMs = 2_000;

        EnvioCorreoException error = assertThrows(EnvioCorreoException.class,
                () -> emisor(Duration.ofMillis(200)).enviarCodigoVerificacion("cliente@test.com", "123456"));

        assertTrue(error.getMessage().toLowerCase().contains("brevo"));
    }

    @Test
    @DisplayName("escapa el nombre del restaurante al rellenar el HTML")
    void escapaElNombreDelRestaurante() throws Exception {
        BrevoProperties propiedades = propiedades(Duration.ofSeconds(2));
        propiedades.setRemitenteNombre("<b>Bar & Grill</b>");
        new EmisorCorreoBrevo(propiedades, JsonMapper.builder().build())
                .enviarCodigoVerificacion("cliente@test.com", "123456");

        String html = JsonMapper.builder().build().readTree(cuerpoRecibido.get())
                .path("htmlContent").asText();
        assertFalse(html.contains("<b>Bar & Grill</b>"));
        assertTrue(html.contains("&lt;b&gt;Bar &amp; Grill&lt;/b&gt;"));
    }

    private EmisorCorreoBrevo emisor(Duration timeout) {
        return new EmisorCorreoBrevo(propiedades(timeout), JsonMapper.builder().build());
    }

    private BrevoProperties propiedades(Duration timeout) {
        BrevoProperties propiedades = new BrevoProperties();
        propiedades.setHabilitado(true);
        propiedades.setApiKey(API_KEY);
        propiedades.setUrl("http://127.0.0.1:" + servidor.getAddress().getPort() + "/v3/smtp/email");
        propiedades.setRemitenteEmail("no-reply@restaurante.test");
        propiedades.setRemitenteNombre("Estación Burger");
        propiedades.setTimeout(timeout);
        propiedades.validar();
        return propiedades;
    }
}
