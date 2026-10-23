package com.restaurante.delivery.application;

import com.restaurante.clientes.PedidoDomicilioSPI;

/**
 * Pedido a domicilio listo para asignar (vista del administrador). Reúne la
 * referencia al pedido del cliente y sus datos de entrega (PII). Solo debe
 * exponerse a usuarios con permiso de administración de asignaciones.
 */
public record PedidoListoParaAsignar(
        Long pedidoClienteId,
        String codigo,
        String clienteNombre,
        String direccion,
        String telefono) {

    static PedidoListoParaAsignar de(PedidoDomicilioSPI p) {
        return new PedidoListoParaAsignar(
                p.pedidoClienteId(),
                p.codigo(),
                p.clienteNombre(),
                p.direccion(),
                p.telefono());
    }
}
