package com.restaurante.clientes;

import java.math.BigDecimal;
import java.util.List;

/** RF-40: carrito del cliente (BORRADOR). Suma congelada con Money SPI.
 *  Respuesta pública (A-09): no incluye clienteId. */
public record CarritoClienteSPI(
        List<PedidoClienteSPI> borradores,
        BigDecimal total,
        int totalItems) {
}