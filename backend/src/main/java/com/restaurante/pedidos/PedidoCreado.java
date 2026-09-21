package com.restaurante.pedidos;

/**
 * Evento publicado al crear un pedido en borrador. Vive en el paquete raíz del
 * módulo para que las cuentas abran (o reutilicen) la cuenta de la mesa sin
 * romper el encapsulamiento de Spring Modulith.
 */
public record PedidoCreado(
        String pedidoCodigo,
        Long mesaId) {
}