package com.restaurante.delivery.domain;

/**
 * El proveedor de rutas (OpenRouteService) no pudo atender la solicitud:
 * timeout, respuesta 4xx/5xx, cuerpo inesperado o {@code api-key} ausente en
 * desarrollo (fail-closed). No es un error de negocio del usuario (eso es
 * {@link com.restaurante.shared.domain.exception.BusinessRuleException}, 422);
 * la capa web la mapeará a 503/502 en la fase de endpoints.
 *
 * <p>No extiende {@code DomainException} a propósito: el manejador global
 * traduce {@code DomainException} a 400, y este fallo es de disponibilidad de
 * un servicio externo, no de la petición. El mensaje nunca incluye la api-key.
 */
public class RutasNoDisponiblesException extends RuntimeException {

    public RutasNoDisponiblesException(String message) {
        super(message);
    }

    public RutasNoDisponiblesException(String message, Throwable cause) {
        super(message, cause);
    }
}
