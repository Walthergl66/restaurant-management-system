package com.restaurante.pedidos;

import java.util.List;

/**
 * Resumen de un pedido para otros módulos, con sus líneas congeladas.
 * El estado viaja como String para que los consumidores no dependan del
 * sub-paquete de dominio (fronteras de Spring Modulith).
 */
public record PedidoResumen(
        String codigo,
        Long mesaId,
        String estado,
        List<LineaResumen> lineas) {
}