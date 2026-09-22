package com.restaurante.clientes;

import com.restaurante.clientes.domain.PedidoClienteTablet;
import com.restaurante.shared.domain.Money;

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

    /** RF-40/41: pedir por c&oacute;digo (men&uacute; RF-43/cat&aacute;logo SPI
     *  RF-09/41/42). */
    PedidoClienteTablet pedidoPorCodigo(String codigo);

    /** RF-44/45: confirmar un pedido del carrito congelando el total
     *  ({@code Money}). Reintento con la MISMA clave idempotente = 200 por
     *  RF-24/25/RF-26/27 RF-44; clave DISTINTA sobre pedido ya confirmado =
     *  409 RF-41/RF-24/25/26/27 RF-44. NUNCA dinero {@code double} (RF-09/41). */
    PedidoClienteTablet confirmar(String codigo, Object idempotencyKey);
}
