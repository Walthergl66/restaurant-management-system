package com.restaurante.anulaciones;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * API pública del módulo de anulaciones. La consume la cuenta para descontar
 * las anulaciones aprobadas del total (RNF-16) y reportes para conciliar las
 * ventas por producto.
 */
public interface Anulaciones {

    /**
     * Anulaciones APROBADAS de los pedidos indicados, para el cálculo de cuenta.
     */
    List<AnulacionResumen> aprobadasDe(List<String> pedidoCodigos);

    /**
     * Monto total aprobado y descontado por producto entre dos instantes
     * (para el reporte de ventas por producto, RF-51).
     */
    List<DescuentoProducto> aprobadasPorProducto(Instant desde, Instant hasta);

    record DescuentoProducto(String productoId, String nombreProducto,
                             int cantidad, BigDecimal monto) {
    }
}
