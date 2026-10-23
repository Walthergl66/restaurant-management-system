package com.restaurante.usuarios.infrastructure;

import com.restaurante.usuarios.CorreoNoEnviadoException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas del adaptador de Resend contra un servidor HTTP stub local (JDK,
 * sin dependencias): se valida el contrato real de la petición —cabeceras,
 * payload, manejo de errores— sin salir de la JVM.
 */
class EmisorCorreoResendTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static HttpServer servidor;
    private static String baseUrl;
    private static volatile int estadoRespuesta;
    private static volatile String cuerpoRespuesta;
    private static final List<Solicitud> solicitudes = new CopyOnWriteArrayList<>();

    /** Una petición capturada por el stub. */
    private record Solicitud(String metodo, String ruta, HttpHeaders headers, String cuerpo) {
    }

    @BeforeAll
    static void arrancaElServidorStub() throws IOException {
        servidor = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        servidor.createContext("/", EmisorCorreoResendTest::responder);
        servidor.start();
        baseUrl = "http://localhost:" + servidor.getAddress().getPort();
    }

    @AfterAll
    static void paraElServidorStub() {
        servidor.stop(0);
    }

    @BeforeEach
    void reiniciaElStub() {
        estadoRespuesta = 201;
        cuerpoRespuesta = "{\"id\":\"msg-1\"}";
        solicitudes.clear();
    }

    private static void responder(HttpExchange intercambio) throws IOException {
        String cuerpo = new String(intercambio.getRequestBody().readAllBytes(),
                StandardCharsets.UTF_8);
        HttpHeaders cabeceras = new HttpHeaders();
        for (Map.Entry<String, List<String>> entrada : intercambio.getRequestHeaders().entrySet()) {
            cabeceras.put(entrada.getKey(), new ArrayList<>(entrada.getValue()));
        }
        solicitudes.add(new Solicitud(intercambio.getRequestMethod(),
                intercambio.getRequestURI().getPath(), cabeceras, cuerpo));
        byte[] respuesta = cuerpoRespuesta.getBytes(StandardCharsets.UTF_8);
        intercambio.getResponseHeaders().set("Content-Type", "application/json");
        intercambio.sendResponseHeaders(estadoRespuesta, respuesta.length);
        intercambio.getResponseBody().write(respuesta);
        intercambio.close();
    }

    private EmisorCorreoResend adaptador(String apiKey, String from, String fromName) {
        MailProperties propiedades = new MailProperties();
        propiedades.setApiKey(apiKey);
        propiedades.setFrom(from);
        propiedades.setFromName(fromName);
        propiedades.setApiUrl(baseUrl);
        return new EmisorCorreoResend(propiedades, MAPPER);
    }

    @Test
    void enviaElPayloadQueResendEspera() {
        adaptador("re_test_123", "no-responder@test.local", "Restaurante Demo")
                .enviarCodigoVerificacion("cliente@test.com", "123456");

        assertEquals(1, solicitudes.size());
        Solicitud solicitud = solicitudes.get(0);
        assertEquals("POST", solicitud.metodo());
        assertEquals("/emails", solicitud.ruta());
        assertEquals("Bearer re_test_123",
                solicitud.headers().getFirst(HttpHeaders.AUTHORIZATION));
        // Resend rechaza con 403/error 1010 cualquier petición sin User-Agent.
        String agente = solicitud.headers().getFirst(HttpHeaders.USER_AGENT);
        assertTrue(agente != null && !agente.isBlank(), "User-Agent obligatorio");

        JsonNode json = MAPPER.readTree(solicitud.cuerpo());
        assertEquals("Restaurante Demo <no-responder@test.local>", json.get("from").asString());
        assertEquals("cliente@test.com", json.get("to").get(0).asString());
        assertFalse(json.get("subject").asString().isBlank());
        String html = json.get("html").asString();
        assertTrue(html.contains("123456"), "el correo lleva el código");
        assertFalse(html.contains(EmisorCorreoResend.MARCADOR_CODIGO), "marcador sustituido");
        assertTrue(html.contains("<html"), "es HTML");
    }

    @Test
    void sinNombreDeRemitenteEnviaSoloLaDireccion() {
        adaptador("re_test", "no-responder@test.local", "")
                .enviarCodigoVerificacion("cliente@test.com", "000111");

        JsonNode json = MAPPER.readTree(solicitudes.get(0).cuerpo());
        assertEquals("no-responder@test.local", json.get("from").asString());
    }

    @Test
    void claveInvalida401NoRevelaElCodigo() {
        estadoRespuesta = 401;
        cuerpoRespuesta = "{\"message\":\"API key is invalid\",\"name\":\"validation_error\"}";

        CorreoNoEnviadoException e = assertThrows(CorreoNoEnviadoException.class,
                () -> adaptador("re_mala", "no-responder@test.local", "")
                        .enviarCodigoVerificacion("cliente@test.com", "123456"));

        assertTrue(e.getMessage().contains("401"));
        assertTrue(e.getMessage().contains("API key is invalid"));
        assertFalse(e.getMessage().contains("123456"), "el código nunca sale en errores");
    }

    @Test
    void destinatarioNoPermitido403SePropaga() {
        estadoRespuesta = 403;
        cuerpoRespuesta = "{\"message\":\"You can only send testing emails to your own email address\"}";

        CorreoNoEnviadoException e = assertThrows(CorreoNoEnviadoException.class,
                () -> adaptador("re_test", "onboarding@resend.dev", "")
                        .enviarCodigoVerificacion("otro@test.com", "654321"));

        assertTrue(e.getMessage().contains("403"));
        assertTrue(e.getMessage().contains("own email address"));
    }

    @Test
    void limiteDeCuota429SePropaga() {
        estadoRespuesta = 429;
        cuerpoRespuesta = "{\"message\":\"You have reached your monthly email quota\"}";

        CorreoNoEnviadoException e = assertThrows(CorreoNoEnviadoException.class,
                () -> adaptador("re_test", "onboarding@resend.dev", "")
                        .enviarCodigoVerificacion("cliente@test.com", "111222"));

        assertTrue(e.getMessage().contains("429"));
    }

    @Test
    void sinConexionConElProvedorNoHayEnvio() {
        MailProperties propiedades = new MailProperties();
        propiedades.setApiKey("re_test");
        propiedades.setFrom("onboarding@resend.dev");
        propiedades.setApiUrl("http://localhost:1");
        propiedades.setConnectTimeoutMs(500);
        propiedades.setReadTimeoutMs(500);
        EmisorCorreoResend sinServidor = new EmisorCorreoResend(propiedades, MAPPER);

        assertThrows(CorreoNoEnviadoException.class,
                () -> sinServidor.enviarCodigoVerificacion("cliente@test.com", "999888"));
    }

    @Test
    void sinClaveApiElArranqueFalla() {
        MailProperties propiedades = new MailProperties();
        propiedades.setApiKey(" ");
        propiedades.setFrom("onboarding@resend.dev");

        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> new EmisorCorreoResend(propiedades, MAPPER));
        assertTrue(e.getMessage().contains("RESEND_API_KEY"));
    }

    @Test
    void sinRemitenteElArranqueFalla() {
        MailProperties propiedades = new MailProperties();
        propiedades.setApiKey("re_test");

        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> new EmisorCorreoResend(propiedades, MAPPER));
        assertTrue(e.getMessage().contains("MAIL_FROM"));
    }
}
