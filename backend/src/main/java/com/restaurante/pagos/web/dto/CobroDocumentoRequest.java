package com.restaurante.pagos.web.dto;

import jakarta.validation.constraints.Size;

/**
 * Datos opcionales del comprobante al cobrar (facultativos: se emite TICKET
 * si no se piden datos fiscales, RF-28 a RF-30).
 */
public record CobroDocumentoRequest(
        @Size(max = 10, message = "Tipo inválido")
        String tipo,

        @Size(max = 120, message = "El nombre no puede superar 120 caracteres")
        String clienteNombre,

        @Size(max = 20, message = "La identificación no puede superar 20 caracteres")
        String clienteIdentificacion) {
}