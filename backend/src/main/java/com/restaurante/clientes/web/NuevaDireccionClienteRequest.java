package com.restaurante.clientes.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** RF-42: alta de dirección para domicilio. */
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
        String observaciones) {
}
