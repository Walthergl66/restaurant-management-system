package com.restaurante.usuarios.infrastructure.correo;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.time.Duration;

/**
 * Configuración del envío de correo por Brevo ({@code app.correo.brevo}).
 *
 * <p>La API key y el remitente llegan por entorno ({@code BREVO_API_KEY},
 * {@code BREVO_REMITENTE_EMAIL}); nunca se escriben en el repositorio. Si
 * {@code habilitado} es {@code true} la aplicación falla al arrancar cuando
 * falta la clave o el remitente, en vez de descubrirlo con el primer registro.
 * Con {@code habilitado=false} el módulo usa el emisor de log (dev y pruebas).
 */
@ConfigurationProperties(prefix = "app.correo.brevo")
public class BrevoProperties {

    /** Activa el adaptador real. En producción debe ser true. */
    private boolean habilitado;

    /** Clave de la API transaccional de Brevo (cabecera {@code api-key}). */
    private String apiKey = "";

    /** Endpoint de envío; se puede sobreescribir para pruebas locales. */
    private String url = "https://api.brevo.com/v3/smtp/email";

    /** Remitente verificado en la cuenta de Brevo. */
    private String remitenteEmail = "";

    /** Nombre visible del remitente y de la marca en el correo. */
    private String remitenteNombre = "Restaurante";

    /** Timeout de conexión y de la petición completa. */
    private Duration timeout = Duration.ofSeconds(5);

    @PostConstruct
    void validar() {
        if (!habilitado) {
            return;
        }
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "BREVO_API_KEY es obligatoria cuando app.correo.brevo.habilitado=true");
        }
        if (remitenteEmail == null || remitenteEmail.isBlank()) {
            throw new IllegalStateException(
                    "BREVO_REMITENTE_EMAIL es obligatoria cuando app.correo.brevo.habilitado=true");
        }
        if (!remitenteEmail.contains("@")) {
            throw new IllegalStateException(
                    "BREVO_REMITENTE_EMAIL no es un correo válido: " + remitenteEmail);
        }
        try {
            URI.create(url);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("app.correo.brevo.url no es una URL válida: " + url, e);
        }
        if (timeout == null || timeout.isNegative() || timeout.isZero()) {
            throw new IllegalStateException("app.correo.brevo.timeout debe ser mayor que cero");
        }
    }

    public boolean isHabilitado() {
        return habilitado;
    }

    public void setHabilitado(boolean habilitado) {
        this.habilitado = habilitado;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getRemitenteEmail() {
        return remitenteEmail;
    }

    public void setRemitenteEmail(String remitenteEmail) {
        this.remitenteEmail = remitenteEmail;
    }

    public String getRemitenteNombre() {
        return remitenteNombre;
    }

    public void setRemitenteNombre(String remitenteNombre) {
        this.remitenteNombre = remitenteNombre;
    }

    public Duration getTimeout() {
        return timeout;
    }

    public void setTimeout(Duration timeout) {
        this.timeout = timeout;
    }
}
