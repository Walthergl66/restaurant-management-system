package com.restaurante.pedidos;

import java.util.List;

/**
 * Evento que se publica al confirmar un pedido. Vive en el paquete raíz del
 * módulo para que otros módulos (comandas) reaccionen sin romper Modulith:
 * se dispara en la MISMÍSIMA transacción de la confirmación y su congelación
 * de precios, así el outbox queda garantizado (RNF-17).
 */
public record PedidoConfirmado(
        String pedidoCodigo,
        List<LineaConfirmada> lineas) {

    public record LineaConfirmada(
            Long productoId,
            String nombreProducto,
            int cantidad,
            List<String> extras,
            List<String> ingredientesRemovidos,
            String observaciones,
            Long areaId,
            String areaNombre) {
    }
}