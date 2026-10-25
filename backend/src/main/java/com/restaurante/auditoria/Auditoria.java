package com.restaurante.auditoria;

/**
 * API pública del módulo de auditoría. Los demás módulos deben usar esta
 * interfaz para registrar eventos, sin violar el encapsulamiento de Spring
 * Modulith.
 */
public interface Auditoria {

    void registrar(String tipo, String entidad, String entidadId, String detalle);

    void registrar(String tipo, String entidad, String entidadId, String detalle, String categoria);

    void registrar(String usuario, String tipo, String entidad, String entidadId, String detalle, String categoria);

}
