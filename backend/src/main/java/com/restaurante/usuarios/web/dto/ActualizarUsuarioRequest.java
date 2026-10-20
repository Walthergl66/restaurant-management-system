package com.restaurante.usuarios.web.dto;

import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Actualización parcial de un usuario: nombre, rol, estado y/o permisos.
 * {@code permisos} en null = no tocar; en lista (aunque vacía) = reemplaza
 * el set explícito del usuario.
 */
public record ActualizarUsuarioRequest(
        @Size(max = 100, message = "El nombre no puede superar 100 caracteres")
        String nombre,
        String rolCodigo,
        Boolean activo,
        List<String> permisos) {
}