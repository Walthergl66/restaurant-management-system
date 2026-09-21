package com.restaurante.pedidos;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * API pública del módulo de pedidos. La consumen comandas (estados de cocina),
 * anulaciones (consultar líneas congeladas), cuentas (agrupar y totalizar) y
 * reportes (ventas por producto), sin violar el encapsulamiento de Spring
 * Modulith.
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
     * Pedidos de la mesa que ya cuentan para la cuenta (confirmados o más
     * avanzados; se excluyen borradores y anulados).
     */
    List<PedidoResumen> pedidosConfirmadosDeMesa(Long mesaId);

    /**
     * True si la mesa tiene otro pedido no anulado distinto del indicado.
     */
    boolean hayOtroPedidoEnMesa(Long mesaId, String pedidoCodigo);

    /**
     * Adición: crea un borrador en una mesa que ya tiene cuenta abierta y
     * devuelve su código (RF-17 a RF-19).
     */
    String crearAdicion(Long mesaId, String codigo, String notas);

    /**
     * Suma vendida por producto (cantidad y monto congelados) de los pedidos
     * confirmados o más avanzados —sin borradores ni anulados— creados en el
     * período. No descuenta anulaciones: eso lo concilia reportes con la API
     * de anulaciones.
     */
    List<VentaProducto> ventasPorProducto(Instant desde, Instant hasta);

    record VentaProducto(String productoId, String nombreProducto,
                         int cantidad, BigDecimal monto) {
    }
}