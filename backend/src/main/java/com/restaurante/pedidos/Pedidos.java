package com.restaurante.pedidos;

import java.util.List;
import java.util.Optional;

/**
 * API pública del módulo de pedidos. La consumen comandas (estados de cocina),
 * anulaciones (consultar líneas congeladas) y cuentas (agrupar y totalizar),
 * sin violar el encapsulamiento de Spring Modulith.
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

    /**
     * Pedido confirmado (o más avanzado) con sus líneas congeladas.
     */
    Optional<PedidoResumen> pedidoConfirmado(String pedidoCodigo);

    /**
     * Todos los pedidos no anulados de una mesa, con sus líneas.
     */
    List<PedidoResumen> pedidosDeMesa(Long mesaId);

    /**
     * True si la mesa tiene otro pedido no anulado distinto del indicado.
     */
    boolean hayOtroPedidoEnMesa(Long mesaId, String pedidoCodigo);

    /**
     * Adición: crea un borrador en una mesa que ya tiene cuenta abierta y
     * devuelve su código (RF-17 a RF-19).
     */
    String crearAdicion(Long mesaId, String codigo, String notas);
}