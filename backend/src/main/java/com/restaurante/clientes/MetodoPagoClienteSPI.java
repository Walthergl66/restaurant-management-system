package com.restaurante.clientes;

import java.time.Instant;

/** RF-45: método de pago guardado por el cliente (solo metadata no sensible). */
public record MetodoPagoClienteSPI(
        Long id,
        Long clienteId,
        String tipo,
        String alias,
        String ultimos4,
        boolean predeterminado,
        Instant creadoAt) {
}
