package com.restaurante.delivery.infrastructure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuración de OpenRouteService ({@code app.ors}). La clave vive SOLO en el
 * backend y llega por entorno ({@code ORS_API_KEY}); nunca se escribe en el
 * repositorio ni en los logs.
 *
 * <p>En desarrollo la clave puede estar vacía: en ese caso el módulo NO llama a
 * ORS y responde fail-closed. No se valida en el arranque (a diferencia de
 * Brevo) porque dev debe poder arrancar sin clave.
 */
@ConfigurationProperties(prefix = "app.ors")
public class OrsProperties {

    /** Clave de la API de ORS (cabecera {@code Authorization}). Vacía en dev. */
    private String apiKey = "";

    /** Endpoint base de ORS; se puede sobreescribir para pruebas locales. */
    private String baseUrl = "https://api.openrouteservice.org";

    /** Timeout de conexión y de lectura, en milisegundos. */
    private int timeoutMs = 8000;

    /** {@code true} si hay clave configurada y se puede llamar a ORS. */
    public boolean tieneApiKey() {
        return apiKey != null && !apiKey.isBlank();
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public int getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(int timeoutMs) {
        this.timeoutMs = timeoutMs;
    }
}
