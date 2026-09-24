package com.restaurante.clientes;

import java.util.List;

/**
 * Evento publicado al confirmar un pedido de la app del cliente, en la
 * MISMÍSIMA transacción de la confirmación (outbox garantizado, RNF-17). Vive
 * en el paquete raíz del módulo para que otros módulos (comandas) reaccionen
 * sin violar Modulith. El área de cada línea se resuelve desde el catálogo en
 * el momento de confirmar, igual que en el flujo presencial.
 */
public record PedidoClienteConfirmado(
        String pedidoCodigo,
        List<LineaConfirmada> lineas) {

    public record LineaConfirmada(
            Long productoId,
            String nombreProducto,
            int cantidad,
            List<String> extras,
            String observaciones,
            Long areaId,
            String areaNombre) {
    }
}