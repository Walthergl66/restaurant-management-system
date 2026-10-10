package com.restaurante.usuarios;

/**
 * Evento de dominio publicado (misma transacción) tras el auto-registro público
 * de un cliente (RF-45). El módulo clientes lo escucha para materializar el
 * perfil con la cédula y el celular reales capturados en el registro, evitando
 * los datos de relleno de la resolución perezosa y acoplar usuarios a clientes.
 */
public record ClienteAutoRegistrado(Long usuarioId, String nombre, String cedula, String celular) {
}
