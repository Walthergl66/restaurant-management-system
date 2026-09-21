package com.restaurante.cuentas.web.dto;

/**
 * Respuesta al crear una adición: el código del nuevo borrador para seguir su
 * personalización y confirmación como cualquier pedido.
 */
public record AdicionResponse(
        String pedidoCodigo,
        Long cuentaId) {
}