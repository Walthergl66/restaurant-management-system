package com.restaurante.catalogo.application;

import com.restaurante.catalogo.domain.Categoria;
import java.text.Collator;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Todos los valores en cero representan el modo alfabético; posiciones positivas, el personalizado. */
final class OrdenCategorias {
    private OrdenCategorias() {}

    static List<Categoria> ordenar(List<Categoria> categorias) {
        Collator nombres = Collator.getInstance(Locale.forLanguageTag("es"));
        nombres.setStrength(Collator.PRIMARY);
        return categorias.stream().sorted(Comparator.comparingInt(Categoria::getOrden)
                .thenComparing(Categoria::getNombre, nombres)
                .thenComparing(Categoria::getId)).toList();
    }
}
