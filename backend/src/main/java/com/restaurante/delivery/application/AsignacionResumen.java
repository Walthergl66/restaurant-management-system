package com.restaurante.delivery.application;

import com.restaurante.delivery.domain.Asignacion;

import java.time.Instant;

/**
 * Estado de reparto de un pedido para el panel del administrador: el estado
 * logístico de la asignación junto a los datos de entrega del pedido (PII).
 * Los campos del cliente pueden ir a {@code null} si el pedido ya no existe
 * o carece de dirección; solo debe exponerse a administración de asignaciones.
 */
public record AsignacionResumen(
        Long asignacionId,
        Long pedidoClienteId,
        Long repartidorId,
        String estado,
        String asignadoPor,
        Instant asignadoAt,
        String codigo,
        String clienteNombre,
        String direccion,
        String telefono) {

    static AsignacionResumen de(Asignacion a, com.restaurante.clientes.PedidoDomicilioSPI datos) {
        return new AsignacionResumen(
                a.getId(),
                a.getPedidoClienteId(),
                a.getRepartidorId(),
                a.getEstado().name(),
                a.getAsignadoPor(),
                a.getAsignadoAt(),
                datos != null ? datos.codigo() : null,
                datos != null ? datos.clienteNombre() : null,
                datos != null ? datos.direccion() : null,
                datos != null ? datos.telefono() : null);
    }
}
