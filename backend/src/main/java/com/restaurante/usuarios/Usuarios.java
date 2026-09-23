package com.restaurante.usuarios;

import java.util.Optional;

/**
 * API pública del módulo usuarios para que otros módulos (clientes, auditoría...)
 * resuelvan usuarios sin romper el encapsulamiento de Spring Modulith.
 */
public interface Usuarios {

    Optional<UsuarioResumen> porUsername(String username);

    record UsuarioResumen(Long id, String username, String nombre, String rolCodigo, boolean activo) {
    }
}
