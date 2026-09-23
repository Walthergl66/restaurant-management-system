package com.restaurante.clientes.domain;

/** RF-40..45: estados del pedido del cliente, misma regla que pedidos: transiciones en una sola clase. */
public enum EstadoPedidoCliente {
    BORRADOR,
    CONFIRMADO,
    EN_PREPARACION,
    LISTO,
    ENTREGADO,
    ANULADO
}
