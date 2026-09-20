package com.restaurante.configuracion.web.dto;

import com.restaurante.configuracion.domain.TipoImpresora;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Solicitud para crear o actualizar una impresora.
 */
public record ImpresoraRequest(
        @NotBlank(message = "El nombre de la impresora es obligatorio")
        @Size(max = 80, message = "El nombre no puede superar 80 caracteres")
        String nombre,

        @NotNull(message = "El tipo de impresora es obligatorio")
        TipoImpresora tipo,

        @Size(max = 45, message = "La IP no puede superar 45 caracteres")
        String ip,

        @Min(value = 1, message = "El puerto debe ser mayor que 0")
        @Max(value = 65535, message = "El puerto no puede superar 65535")
        Integer puerto,

        @Size(max = 100, message = "El área no puede superar 100 caracteres")
        String area) {
}