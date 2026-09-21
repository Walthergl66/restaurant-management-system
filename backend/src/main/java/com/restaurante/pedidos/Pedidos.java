package com.restaurante.pedidos;

/**
 * API pública del módulo de pedidos. La consume el módulo de comandas para
 * avanzar el estado del pedido según la cocina, sin violar el encapsulamiento
 * de Spring Modulith.
 */
public interface Pedidos {

    /**
     * Marca el pedido como en preparación (si aún no lo está).
     */
    void marcarEnPreparacion(String pedidoCodigo);

    /**
     * Marca el pedido como listo (si aún no lo está).
     */
    void marcarListo(String pedidoCodigo);
}