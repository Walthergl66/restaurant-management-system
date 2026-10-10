package com.restaurante.clientes;

/**
 * Perfil del cliente autenticado (RF-45). SPI público del módulo clientes.
 */
public record ClientePerfilSPI(
        Long id,
        Long usuarioId,
        String nombre,
        String cedula,
        String telefono) {
}
