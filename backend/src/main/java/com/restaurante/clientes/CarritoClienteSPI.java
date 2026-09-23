package com.restaurante.clientes;

import java.math.BigDecimal;
import java.util.List;

/** RF-40: carrito del cliente (BORRADOR). Suma congelada con Money SPI. */
public record CarritoClienteSPI(
        Long clienteId,
        List<PedidoClienteSPI> borradores,
        BigDecimal total,
        int totalItems) {
}
