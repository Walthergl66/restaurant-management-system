package com.restaurante.usuarios.web.dto;

import com.restaurante.usuarios.domain.Usuario;

import java.util.Set;

/**
 * Vista de un usuario para administración. {@code permisos} son los permisos
 * explícitos asignados por el administrador (vacío = defaults del rol).
 */
public record UsuarioAdminResponse(
        Long id,
        String username,
        String nombre,
        boolean activo,
        String rol,
        Set<String> permisos) {

    public static UsuarioAdminResponse from(Usuario usuario) {
        return new UsuarioAdminResponse(
                usuario.getId(),
                usuario.getUsername(),
                usuario.getNombre(),
                usuario.isActivo(),
                usuario.getRol() == null ? null : usuario.getRol().getCodigo(),
                usuario.getPermisosExplicitosCodigos());
    }
}