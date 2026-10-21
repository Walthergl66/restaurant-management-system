package com.restaurante.usuarios.web.dto;

import java.util.Set;

/**
 * Rol con sus permisos para consulta.
 *
 * @param id identificador del rol
 * @param codigo código del rol (MESERO, CAJERO, ADMIN, CLIENTE, DELIVERY)
 * @param descripcion descripción del rol
 * @param categoria código de la categoría a la que pertenece el rol
 * @param categoriaDescripcion descripción de esa categoría
 * @param asignablePorAdmin si el administrador puede crear usuarios con este
 *                          rol desde el panel (ADMIN, CLIENTE y DELIVERY no)
 * @param configurable si el administrador puede ajustar las funcionalidades
 *                     (permisos explícitos) de los usuarios con este rol
 * @param origen de dónde nace la cuenta con este rol (ADMIN o AUTO_REGISTRO)
 * @param permisos funcionalidades por defecto del rol
 */
public record RolDto(
        Long id,
        String codigo,
        String descripcion,
        String categoria,
        String categoriaDescripcion,
        boolean asignablePorAdmin,
        boolean configurable,
        String origen,
        Set<String> permisos) {
}
