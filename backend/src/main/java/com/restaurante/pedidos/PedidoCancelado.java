package com.restaurante.pedidos;

/**
 * Evento publicado al cancelar un pedido en borrador. Las cuentas lo usan para
 * cerrar una cuenta vacía cuando ya no queda ningún pedido en la mesa.
 */
public record PedidoCancelado(
        String pedidoCodigo,
        Long mesaId) {
}