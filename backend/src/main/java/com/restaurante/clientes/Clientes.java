package com.restaurante.clientes;

import com.restaurante.catalogo.ProductoParaPedido;
import com.restaurante.clientes.domain.PedidoClienteTablet;
import com.restaurante.clientes.web.ConfirmarPedidoClienteRequest;
import com.restaurante.clientes.web.CrearPedidoClienteRequest;
import com.restaurante.clientes.web.NuevaDireccionClienteRequest;

import java.util.List;

/** RF-43/RF-40 a RF-45: SPI &uacute;nico que la app del cliente (RF-40..45) y
 *  la tablet del mesero (RF-44/45 RF-24/25) consumen del m&oacute;dulo
 *  clientes. Contrato p&uacute;blico sin importar paquetes internos
 *  {@code com.restaurante.clientes.domain|application|web|infrastructure}
 *  (RF-06/09/22/23/26/41 regla de modularidad RF-22/23).

 *  <p>Regla &uacute;nica de dinero (RF-09/22/23/26/41/44 RF-40..45): todo monto
 *  del carrito/pedido se congela con {@code Money} del SPI shared (nunca
 *  {@code double}, {@code BigDecimal} suelto o {@code double}) — RF-41
 *  confirmaci&oacute;n congelada idempotente RF-24/25.</p>
 */
public interface Clientes {

    /** RF-40: carrito del cliente (BORRADOR). */
    CarritoClienteSPI carrito(Long clienteId);

    /** RF-40: menú visible desde la app (catálogo SPI). */
    List<ProductoParaPedido> menu();

    /** RF-41: crear pedido (carrito -> BORRADOR) con idempotencia. */
    PedidoClienteSPI crearPedido(Long clienteId, CrearPedidoClienteRequest request);

    /** RF-41: confirmar con Money congelado, idempotente 200 / 409.
     *  Autorización por propietario: el pedido debe pertenecer a clienteId (A-01). */
    PedidoClienteSPI confirmar(Long clienteId, String codigo, ConfirmarPedidoClienteRequest request);

    /** RF-41 legacy: confirmar con clave suelta (tablet). */
    PedidoClienteTablet confirmar(String codigo, Object idempotencyKey);

    /** RF-40/41: pedir por c&oacute;digo (men&uacute; RF-43/cat&aacute;logo SPI
     *  RF-09/41/42). */
    PedidoClienteTablet pedidoPorCodigo(String codigo);

    /** RF-41 SPI para el controlador (estado en vivo). Autorización por
     *  propietario: el pedido debe pertenecer a clienteId (A-01). */
    PedidoClienteSPI pedidoSPIporCodigo(Long clienteId, String codigo);

    /** RF-44: tablet marca EN_PREPARACION (RF-24). */
    PedidoClienteSPI marcarEnPreparacion(String codigo);

    /** RF-44: tablet marca LISTO (RF-25). */
    PedidoClienteSPI marcarListo(String codigo);

    /** RF-45: historial del cliente. */
    List<PedidoClienteSPI> historial(Long clienteId);

    /** RF-42: nueva dirección para domicilio. */
    DireccionClienteSPI nuevaDireccion(Long clienteId, NuevaDireccionClienteRequest request);

    /** Resuelve el clienteId real (BD) a partir del username del JWT. */
    Long resolverClienteId(String username);
}
