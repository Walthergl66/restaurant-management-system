package com.restaurante.clientes.web;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** RF-42: alta de dirección para domicilio (con coordenadas del marcador). */
public record NuevaDireccionClienteRequest(
        @NotBlank(message = "La etiqueta es obligatoria")
        @Size(max = 40)
        String etiqueta,

        @NotBlank(message = "La dirección es obligatoria")
        @Size(max = 200)
        String direccion,

        @Size(max = 20)
        String telefono,

        @Size(max = 200)
        String observaciones,

        /** Latitud confirmada del marcador (opcional; validada si viene). */
        @DecimalMin(value = "-90.0", message = "Latitud fuera de rango")
        @DecimalMax(value = "90.0", message = "Latitud fuera de rango")
        BigDecimal lat,

        /** Longitud confirmada del marcador (opcional; validada si viene). */
        @DecimalMin(value = "-180.0", message = "Longitud fuera de rango")
        @DecimalMax(value = "180.0", message = "Longitud fuera de rango")
        BigDecimal lng) {
}
