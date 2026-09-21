package com.restaurante.usuarios.web.dto;

import com.restaurante.usuarios.domain.Rol;
import com.restaurante.usuarios.domain.Usuario;

import java.util.Set;

/**
 * Vista pública de un usuario para la API (nunca expone el hash de contraseña).
 */
public record UsuarioInfo(
        Long id,
        String username,
        String nombre,
        boolean activo,
        String rol,
        Set<String> permisos) {

    public static UsuarioInfo from(Usuario usuario) {
        Rol rol = usuario.getRol();
        return new UsuarioInfo(
                usuario.getId(),
                usuario.getUsername(),
                usuario.getNombre(),
                usuario.isActivo(),
                rol == null ? null : rol.getCodigo(),
                usuario.getPermisoCodigos());
    }
}