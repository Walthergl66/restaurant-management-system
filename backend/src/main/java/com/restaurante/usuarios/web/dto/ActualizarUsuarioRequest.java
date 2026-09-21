package com.restaurante.usuarios.web.dto;

import jakarta.validation.constraints.Size;

/**
 * Actualización parcial de un usuario: nombre, rol y/o estado.
 */
public record ActualizarUsuarioRequest(
        @Size(max = 100, message = "El nombre no puede superar 100 caracteres")
        String nombre,
        String rolCodigo,
        Boolean activo) {
}