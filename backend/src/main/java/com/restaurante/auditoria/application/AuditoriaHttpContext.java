package com.restaurante.auditoria.application;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import java.util.UUID;

/**
 * Captura datos del request HTTP para enriquecer la auditoría con IP,
 * User-Agent y un requestId correlacionable con logs.
 */
@Component
@RequestScope
public class AuditoriaHttpContext {

    private String ip;
    private String userAgent;
    private String requestId;
    private String uri;
    private String metodo;

    public AuditoriaHttpContext(HttpServletRequest request) {
        this.uri = request.getRequestURI();
        this.metodo = request.getMethod();
        this.userAgent = recortar(request.getHeader("User-Agent"), 255);
        this.ip = extraerIp(request);
        this.requestId = UUID.randomUUID().toString();
    }

    private String extraerIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            String primera = xff.split(",")[0].trim();
            if (!primera.isBlank()) {
                return primera;
            }
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }

    private String recortar(String valor, int max) {
        if (valor == null) {
            return null;
        }
        return valor.length() > max ? valor.substring(0, max) : valor;
    }

    public String getIp() {
        return ip;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public String getRequestId() {
        return requestId;
    }

    public String getUri() {
        return uri;
    }

    public String getMetodo() {
        return metodo;
    }
}
