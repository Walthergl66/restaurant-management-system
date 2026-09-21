package com.restaurante.pedidos;

import com.restaurante.pedidos.domain.EstadoPedido;

import java.util.List;

/**
 * Resumen de un pedido para otros módulos, con sus líneas congeladas.
 */
public record PedidoResumen(
        String codigo,
        Long mesaId,
        EstadoPedido estado,
        List<LineaResumen> lineas) {
}