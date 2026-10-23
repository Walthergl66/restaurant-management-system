package com.restaurante.usuarios.infrastructure;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuración del envío de correos transaccionales (RF-45).
 * <p>
 * Las propiedades se leen de {@code app.mail.*} y cada una toma su valor de la
 * variable de entorno homóloga ({@link #enabled MAIL_ENABLED},
 * {@link #apiKey RESEND_API_KEY}, {@link #from MAIL_FROM},
 * {@link #fromName MAIL_FROM_NAME}); el resto tienen default razonable. El
 * interruptor {@code MAIL_ENABLED} es fail-closed: sin declararlo, apagado.
 * <p>
 * La validación de que la clave y el remitente existen cuando el envío está
 * encendido vive en {@link EmisorCorreoResend} (solo se exige lo que se va a
 * usar); en producción, además, {@code ProdSecretsValidator} detiene el
 * arranque si falta alguna.
 */
@Component
@ConfigurationProperties(prefix = "app.mail")
public class MailProperties {

    /** false (defecto) = adaptador de log; true = envío real por API. */
    private boolean enabled = false;

    /** Clave API del proveedor (Resend: {@code re_...}); obligatoria si enabled. */
    private String apiKey = "";

    /** Remitente; obligatorio si enabled. Sin dominio: onboarding@resend.dev. */
    private String from = "";

    /** Nombre visible del remitente (opcional; vacío = solo la dirección). */
    private String fromName = "";

    /** Base URL de la API; sobreescrible para pruebas o proxies. */
    private String apiUrl = "https://api.resend.com";

    /** Timeout de conexión HTTP (ms). Corto a propósito: no colgar peticiones. */
    private long connectTimeoutMs = 5_000;

    /** Timeout de lectura HTTP (ms). */
    private long readTimeoutMs = 10_000;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getFromName() {
        return fromName;
    }

    public void setFromName(String fromName) {
        this.fromName = fromName;
    }

    public String getApiUrl() {
        return apiUrl;
    }

    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
    }

    public long getConnectTimeoutMs() {
        return connectTimeoutMs;
    }

    public void setConnectTimeoutMs(long connectTimeoutMs) {
        this.connectTimeoutMs = connectTimeoutMs;
    }

    public long getReadTimeoutMs() {
        return readTimeoutMs;
    }

    public void setReadTimeoutMs(long readTimeoutMs) {
        this.readTimeoutMs = readTimeoutMs;
    }
}
