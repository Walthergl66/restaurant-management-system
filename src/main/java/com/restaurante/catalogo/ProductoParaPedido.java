package com.restaurante.catalogo;

import com.restaurante.shared.domain.Money;

/**
 * Vista fiel del catálogo para congelar en una línea de pedido.
 */
public record ProductoParaPedido(
        Long id,
        String nombre,
        Money precio,
        Long areaId,
        String areaNombre,
        boolean activo) {
}