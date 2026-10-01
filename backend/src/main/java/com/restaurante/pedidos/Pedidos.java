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
     * True si existe un pedido presencial con ese código (las comandas de la
     * app del cliente lo usan para decidir a qué módulo delegar los estados).
     */
    boolean existe(String pedidoCodigo);

    /**
     * Marca el pedido como listo (si aún no lo está).
     */
    void marcarListo(String pedidoCodigo);

    /**
     * Pedido confirmado (o más avanzado) con sus líneas congeladas.
     */
    Optional<PedidoResumen> pedidoConfirmado(String pedidoCodigo);

    /**
     * Igual que {@link #pedidoConfirmado} pero bloqueando el pedido para
     * escritura mientras dure la transacción. Lo usan las operaciones que
     * recalculan un saldo agregado de sus líneas (anulaciones): sin el lock,
     * dos resoluciones simultáneas leen el mismo saldo y ambas lo pasan por
     * alto, con lo cual la cuenta queda descuenciada de más.
     */
    Optional<PedidoResumen> pedidoConfirmadoParaActualizar(String pedidoCodigo);

    /**
     * Todos los pedidos no anulados de una mesa, con sus líneas.
     */
    List<PedidoResumen> pedidosDeMesa(Long mesaId);

    /**
     * Liga el pedido a la cuenta del turno. Lo invoca el módulo de cuentas al
     * abrir (o reutilizar) la cuenta de la mesa, de modo que la pertenencia
     * pedido→cuenta quede fijada en el momento de crear el pedido.
     */
    void asignarCuenta(String pedidoCodigo, Long cuentaId);

    /**
     * Pedidos de la cuenta (no anulados, con sus líneas).
     */
    List<PedidoResumen> pedidosDeCuenta(Long cuentaId);

    /**
     * Pedidos de la cuenta que ya cuentan para su total (confirmados o más
     * avanzados; se excluyen borradores y anulados). Es lo que permite que una
     * cuenta cierre y otra empiece en la misma mesa sin sumar lo ya cobrado.
     */
    List<PedidoResumen> pedidosConfirmadosDeCuenta(Long cuentaId);

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
     * período. El monto de cada línea es su subtotal congelado, es decir,
     * incluye sus extras. No descuenta anulaciones: eso lo concilia reportes
     * con la API de anulaciones.
     */
    List<VentaProducto> ventasPorProducto(Instant desde, Instant hasta);

    record VentaProducto(String productoId, String nombreProducto,
                         int cantidad, BigDecimal monto) {
    }
}
