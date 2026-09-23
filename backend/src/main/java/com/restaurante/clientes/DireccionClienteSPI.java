package com.restaurante.clientes;

import java.time.Instant;

/** RF-42: dirección del cliente para domicilio. */
public record DireccionClienteSPI(
        Long id,
        Long clienteId,
        String etiqueta,
        String direccion,
        String telefono,
        String observaciones,
        boolean activa,
        Instant creadoAt) {
}
