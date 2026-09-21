package com.restaurante.usuarios.web.dto;

import java.util.Set;

/**
 * Rol con sus permisos para consulta.
 */
public record RolDto(
        Long id,
        String codigo,
        String descripcion,
        Set<String> permisos) {
}