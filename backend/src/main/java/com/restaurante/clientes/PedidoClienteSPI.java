package com.restaurante.clientes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** RF-41/RF-43/RF-44/RF-45: vista congelada del pedido del cliente.
 *  Respuesta p&uacute;blica (A-09): solo datos operativos m&iacute;nimos; jam&aacute;s
 *  expone {@code idempotencyKey}, {@code clienteId} ni {@code direccionId}. */
public record PedidoClienteSPI(
        String codigo,
        String estado,
        String metodoPago,
        String metodoEntrega,
        BigDecimal total,
        List<LineaSPI> lineas,
        Instant creadoAt,
        Instant actualizadoAt,
        long version) {

    public record LineaSPI(
            Long productoId,
            String nombre,
            BigDecimal precio,
            int cantidad,
            BigDecimal subtotal,
            List<ExtraSPI> extras) {

        public record ExtraSPI(Long extraId, String nombre, BigDecimal precio) {
        }
    }
}