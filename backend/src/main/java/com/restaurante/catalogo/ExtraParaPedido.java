package com.restaurante.catalogo;

import com.restaurante.shared.domain.Money;

/**
 * Vista fiel de un extra para congelar en una línea de pedido.
 */
public record ExtraParaPedido(
        Long id,
        String nombre,
        Money precio,
        boolean activo) {
}