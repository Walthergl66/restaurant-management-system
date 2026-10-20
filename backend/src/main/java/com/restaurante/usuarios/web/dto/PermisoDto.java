package com.restaurante.usuarios.web.dto;

/**
 * Permiso del catálogo para el checklist de funcionalidades del panel.
 */
public record PermisoDto(
        String codigo,
        String modulo,
        String descripcion) {
}
