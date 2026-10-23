package com.restaurante.usuarios.infrastructure;

import com.restaurante.usuarios.CorreoNoEnviadoException;
import com.restaurante.usuarios.EmisorCorreo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Adaptador de {@link EmisorCorreo} que envía el código de verificación por la
 * API de Resend (POST {@code /emails} con cabecera {@code Authorization:
 * Bearer}). Solo existe cuando {@code app.mail.enabled=true}; con el
 * interruptor apagado (defecto) el rol lo tiene {@link EmisorCorreoLog}, de
 * modo que "sin configurar nada, nada cambia".
 * <p>
 * Decisiones:
 * <ul>
 *   <li><b>User-Agent obligatorio</b>: Resend rechaza con 403 (error 1010)
 *       cualquier petición sin esta cabecera.</li>
 *   <li><b>Timeouts cortos</b> ({@code app.mail.connect/read-timeout-ms}): un
 *       correo no debe colgar la petición del cliente.</li>
 *   <li><b>Fail-fast en el arranque</b>: sin clave o sin remitente, la clase
 *       no se construye y Spring detiene el arranque con mensaje claro.</li>
 *   <li><b>Nunca registra el código</b> (RNF-10): solo el destino, en log de
 *       éxito; los errores se devuelven como {@link CorreoNoEnviadoException}
 *       para que la capa de aplicación decida (Fase 3: auditoría, sin revertir
 *       el registro).</li>
 * </ul>
 */
@Component
@ConditionalOnProperty(prefix = "app.mail", name = "enabled", havingValue = "true")
public class EmisorCorreoResend implements EmisorCorreo {

    private static final Logger log = LoggerFactory.getLogger(EmisorCorreoResend.class);

    /** Plantilla HTML del correo (classpath). El marcador admite solo dígitos. */
    static final String RUTA_PLANTILLA = "templates/mail/codigo-verificacion.html";
    static final String MARCADOR_CODIGO = "{{codigo}}";
    static final String ASUNTO = "Tu código de verificación";

    /**
     * User-Agent fijo: Resend exige cabecera User-Agent (403/error 1010 sin
     * ella); se identifica la integración para poder reconocerla en el panel.
     */
    static final String AGENTE = "restaurant-management-system/1.0";

    private final MailProperties propiedades;
    private final ObjectMapper mapper;
    private final RestClient cliente;
    private final String plantilla;

    public EmisorCorreoResend(MailProperties propiedades, ObjectMapper mapper) {
        this.propiedades = propiedades;
        this.mapper = mapper;
        validar(propiedades);
        this.plantilla = cargarPlantilla();
        this.cliente = RestClient.builder()
                .baseUrl(propiedades.getApiUrl())
                // La clave se recorta: un espacio o salto de línea al pegarla
                // es la causa número uno de 401 con credenciales correctas.
                .defaultHeader(HttpHeaders.AUTHORIZATION,
                        "Bearer " + propiedades.getApiKey().trim())
                .defaultHeader(HttpHeaders.USER_AGENT, AGENTE)
                .requestFactory(nuevaFactory(propiedades))
                .build();
    }

    @Override
    public void enviarCodigoVerificacion(String correo, String codigo) {
        Map<String, Object> peticion = new LinkedHashMap<>();
        peticion.put("from", remitente());
        peticion.put("to", List.of(correo));
        peticion.put("subject", ASUNTO);
        peticion.put("html", plantilla.replace(MARCADOR_CODIGO, codigo));

        try {
            cliente.post()
                    .uri("/emails")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(serializar(peticion))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException e) {
            throw new CorreoNoEnviadoException(
                    "Resend rechazó el correo de verificación para '%s' (HTTP %d): %s"
                            .formatted(correo, e.getStatusCode().value(),
                                    recortar(e.getResponseBodyAsString())),
                    e);
        } catch (RestClientException e) {
            throw new CorreoNoEnviadoException(
                    "No se pudo contactar con Resend para el correo de '%s'"
                            .formatted(correo),
                    e);
        }
        // Sin el código en el log (RNF-10): solo a dónde fue.
        log.info("Código de verificación enviado a '{}'", correo);
    }

    /** {@code Nombre <direccion>} o la dirección si no hay nombre. */
    private String remitente() {
        String direccion = propiedades.getFrom().trim();
        String nombre = propiedades.getFromName() == null
                ? "" : propiedades.getFromName().trim();
        return nombre.isEmpty() ? direccion : nombre + " <" + direccion + ">";
    }

    private String serializar(Map<String, Object> peticion) {
        try {
            return mapper.writeValueAsString(peticion);
        } catch (Exception e) {
            throw new CorreoNoEnviadoException(
                    "No se pudo serializar la petición de correo", e);
        }
    }

    /** Cuerpo de error a lo sumo en una línea y truncado: diagnóstico sin desbordar. */
    private static String recortar(String cuerpo) {
        if (cuerpo == null || cuerpo.isBlank()) {
            return "sin detalle";
        }
        String unaLinea = cuerpo.replaceAll("\\s+", " ").trim();
        return unaLinea.length() > 200 ? unaLinea.substring(0, 200) + "…" : unaLinea;
    }

    private static void validar(MailProperties p) {
        if (p.getApiKey() == null || p.getApiKey().isBlank()) {
            throw new IllegalStateException(
                    "app.mail.enabled=true pero falta la clave API: define RESEND_API_KEY "
                            + "(o desactiva el envío con MAIL_ENABLED=false)");
        }
        if (p.getFrom() == null || p.getFrom().isBlank()) {
            throw new IllegalStateException(
                    "app.mail.enabled=true pero falta el remitente: define MAIL_FROM "
                            + "(p.ej. onboarding@resend.dev) o desactiva con MAIL_ENABLED=false");
        }
        if (p.getApiUrl() == null || p.getApiUrl().isBlank()) {
            throw new IllegalStateException(
                    "app.mail.apiUrl vacío: define MAIL_API_URL o deja el default");
        }
    }

    private static String cargarPlantilla() {
        try (InputStream entrada = new ClassPathResource(RUTA_PLANTILLA).getInputStream()) {
            String html = new String(entrada.readAllBytes(), StandardCharsets.UTF_8);
            if (!html.contains(MARCADOR_CODIGO)) {
                throw new IllegalStateException(
                        "La plantilla " + RUTA_PLANTILLA + " no contiene el marcador " + MARCADOR_CODIGO);
            }
            return html;
        } catch (IOException e) {
            throw new UncheckedIOException(
                    new IOException("No se pudo leer la plantilla " + RUTA_PLANTILLA, e));
        }
    }

    /** Cliente HTTP JDK con timeout de conexión y de lectura. */
    private static ClientHttpRequestFactory nuevaFactory(MailProperties p) {
        HttpClient http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(p.getConnectTimeoutMs()))
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(http);
        factory.setReadTimeout(Duration.ofMillis(p.getReadTimeoutMs()));
        return factory;
    }
}
