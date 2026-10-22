package com.restaurante.auditoria.application;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

/**
 * Captura datos del request HTTP para enriquecer la auditoría con IP,
 * User-Agent y un requestId correlacionable con logs.
 */
@Component
public class AuditoriaHttpContext {

    private final ThreadLocal<Datos> cache = ThreadLocal.withInitial(Datos::new);

    private static class Datos {
        String ip;
        String userAgent;
        String requestId = UUID.randomUUID().toString();
        String uri;
        String metodo;
    }

    private Datos datos() {
        Datos d = cache.get();
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                if (d.uri == null) {
                    d.uri = request.getRequestURI();
                }
                if (d.metodo == null) {
                    d.metodo = request.getMethod();
                }
                if (d.userAgent == null) {
                    d.userAgent = recortar(request.getHeader("User-Agent"), 255);
                }
                if (d.ip == null) {
                    d.ip = extraerIp(request);
                }
                if (d.requestId == null) {
                    d.requestId = UUID.randomUUID().toString();
                }
            }
        } catch (Exception ignored) {
            // Sin contexto HTTP (tests, listeners fuera de request)
        }
        return d;
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
        return datos().ip;
    }

    public String getUserAgent() {
        return datos().userAgent;
    }

    public String getRequestId() {
        Datos d = datos();
        if (d.requestId == null) {
            d.requestId = UUID.randomUUID().toString();
        }
        return d.requestId;
    }

    public String getUri() {
        return datos().uri;
    }

    public String getMetodo() {
        return datos().metodo;
    }
}
