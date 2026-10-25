package com.restaurante.usuarios.infrastructure.correo;

import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Cliente mínimo de la API transaccional de Brevo
 * ({@code POST /v3/smtp/email}). Se usa el {@link HttpClient} del JDK para no
 * arrastrar el SDK oficial solo por un endpoint; el JSON lo arma Jackson 3.
 *
 * <p>La API key viaja en la cabecera {@code api-key} y nunca se incluye en los
 * mensajes de error. Un timeout o una respuesta distinta de 2xx se convierten
 * en {@link EnvioCorreoException}.
 */
public class ClienteBrevo {

    /** El cuerpo de error de Brevo puede ser largo; se recorta para el log. */
    private static final int MAX_DETALLE_ERROR = 300;

    private final BrevoProperties propiedades;
    private final ObjectMapper objectMapper;
    private final HttpClient http;

    public ClienteBrevo(BrevoProperties propiedades, ObjectMapper objectMapper) {
        this(propiedades, objectMapper, HttpClient.newBuilder()
                .connectTimeout(propiedades.getTimeout())
                .build());
    }

    ClienteBrevo(BrevoProperties propiedades, ObjectMapper objectMapper, HttpClient http) {
        this.propiedades = propiedades;
        this.objectMapper = objectMapper;
        this.http = http;
    }

    /**
     * @param etiqueta nombre corto para las estadísticas de Brevo
     *                 (p. ej. {@code verificacion-email})
     */
    public void enviar(String destinatario, String asunto, String html, String texto, String etiqueta) {
        String cuerpo = objectMapper.writeValueAsString(new Peticion(
                new Remitente(propiedades.getRemitenteEmail(), propiedades.getRemitenteNombre()),
                List.of(new Destinatario(destinatario)),
                asunto,
                html,
                texto,
                List.of(etiqueta)));

        HttpRequest peticion = HttpRequest.newBuilder(URI.create(propiedades.getUrl()))
                .timeout(propiedades.getTimeout())
                .header("api-key", propiedades.getApiKey())
                .header("Content-Type", "application/json; charset=utf-8")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(cuerpo, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> respuesta;
        try {
            respuesta = http.send(peticion, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new EnvioCorreoException("No se pudo conectar con Brevo: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new EnvioCorreoException("Envío de correo interrumpido", e);
        }

        if (respuesta.statusCode() / 100 != 2) {
            throw new EnvioCorreoException("Brevo respondió " + respuesta.statusCode()
                    + ": " + truncar(respuesta.body()));
        }
    }

    private static String truncar(String detalle) {
        if (detalle == null || detalle.isBlank()) {
            return "(sin detalle)";
        }
        String limpio = detalle.strip().replaceAll("\\s+", " ");
        return limpio.length() <= MAX_DETALLE_ERROR
                ? limpio
                : limpio.substring(0, MAX_DETALLE_ERROR) + "…";
    }

    /** Cuerpo que espera Brevo en {@code /v3/smtp/email}. */
    private record Peticion(Remitente sender,
                            List<Destinatario> to,
                            String subject,
                            String htmlContent,
                            String textContent,
                            List<String> tags) {
    }

    private record Remitente(String email, String name) {
    }

    private record Destinatario(String email) {
    }
}
