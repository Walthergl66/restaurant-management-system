package com.restaurante.catalogo;

import com.restaurante.shared.domain.Money;
import com.restaurante.shared.domain.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ProductoParaPedidoTest {
    private final ProductoParaPedido producto = new ProductoParaPedido(
            1L, "Hamburguesa", Money.of("3.75"), 1L, "Cocina", true,
            List.of("Cebolla", "Tomate"));

    @Test void permiteOmitirLaPersonalizacion() {
        assertEquals(List.of(), producto.validarIngredientesRemovidos(null));
    }

    @Test void validaYDeduplicaSinCambiarPrecio() {
        assertEquals(List.of("Cebolla"), producto.validarIngredientesRemovidos(List.of("Cebolla", "Cebolla")));
        assertEquals(Money.of("3.75"), producto.precio());
    }

    @Test void rechazaIngredientesAjenos() {
        assertThrows(BusinessRuleException.class,
                () -> producto.validarIngredientesRemovidos(List.of("Tocino")));
    }

    @Test void noReescribeNombresDelCatalogo() {
        assertThrows(BusinessRuleException.class,
                () -> producto.validarIngredientesRemovidos(List.of("cebolla")));
    }
}
