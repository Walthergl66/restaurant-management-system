package com.restaurante.catalogo;

import com.restaurante.shared.domain.Money;
import java.util.List;
import com.restaurante.shared.domain.exception.BusinessRuleException;

/**
 * Vista fiel del catálogo para congelar en una línea de pedido.
 */
public record ProductoParaPedido(
        Long id,
        String nombre,
        Money precio,
        Long areaId,
        String areaNombre,
        boolean activo,
        List<String> ingredientesRemovibles) {
    public ProductoParaPedido {
        ingredientesRemovibles = List.copyOf(ingredientesRemovibles);
    }

    /** Valida nombres exactos publicados por el menú, sin alterar tildes ni el precio. */
    public List<String> validarIngredientesRemovidos(List<String> nombres) {
        if (nombres == null) return List.of();
        if (nombres.stream().anyMatch(nombre -> nombre == null || !ingredientesRemovibles.contains(nombre))) {
            throw new BusinessRuleException("Un ingrediente indicado no es removible para el producto '" + nombre + "'");
        }
        return nombres.stream().distinct().toList();
    }
}
