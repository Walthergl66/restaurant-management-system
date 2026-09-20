package com.restaurante.usuarios.web.dto;

import com.restaurante.usuarios.domain.Usuario;

/**
 * Vista de un usuario para administración.
 */
public record UsuarioAdminResponse(
        Long id,
        String username,
        String nombre,
        boolean activo,
        String rol) {

    public static UsuarioAdminResponse from(Usuario usuario) {
        return new UsuarioAdminResponse(
                usuario.getId(),
                usuario.getUsername(),
                usuario.getNombre(),
                usuario.isActivo(),
                usuario.getRol() == null ? null : usuario.getRol().getCodigo());
    }
}