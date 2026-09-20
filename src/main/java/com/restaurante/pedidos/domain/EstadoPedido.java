package com.restaurante.pedidos.domain;

/**
 * Los únicos estados de un pedido. Las transiciones viven en una sola clase:
 * {@link Pedido}. Un pedido confirmado queda bloqueado (no admite cambios) y
 * el precio de sus líneas queda congelado.
 */
public enum EstadoPedido {
    BORRADOR,
    CONFIRMADO,
    EN_PREPARACION,
    LISTO,
    ENTREGADO,
    ANULADO
}