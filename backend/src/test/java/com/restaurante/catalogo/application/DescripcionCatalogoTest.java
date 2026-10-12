package com.restaurante.catalogo.application;

import com.restaurante.catalogo.domain.*;
import com.restaurante.shared.domain.Money;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class DescripcionCatalogoTest {
    @Test
    void capitalizaYConservaMarcasTildesYParrafosEnTodoElCatalogo() {
        String entrada = "  hamburguesa   con BBQ y piña.\nIncluye Coca-Cola.  ";
        String esperado = "Hamburguesa con BBQ y piña.\nIncluye Coca-Cola.";
        assertThat(TextoCatalogo.normalizar(entrada, TextoCatalogo.Tipo.DESCRIPCION)).isEqualTo(esperado);
        Categoria categoria = new Categoria("Prueba", 0);
        Area area = new Area("Prueba");
        Extra extra = new Extra("Prueba", Money.ZERO);
        Producto producto = new Producto("Prueba", Money.ZERO);
        categoria.cambiarDescripcion(entrada);
        area.cambiarDescripcion(entrada);
        extra.cambiarDescripcion(entrada);
        producto.cambiarDescripcion(entrada);
        assertThat(categoria.getDescripcion()).isEqualTo(esperado);
        assertThat(area.getDescripcion()).isEqualTo(esperado);
        assertThat(extra.getDescripcion()).isEqualTo(esperado);
        assertThat(producto.getDescripcion()).isEqualTo(esperado);
        assertThat(TextoCatalogo.normalizar(esperado, TextoCatalogo.Tipo.DESCRIPCION)).isEqualTo(esperado);
    }

    @Test
    void descripcionEsOpcional() {
        assertThat(TextoCatalogo.normalizar(null, TextoCatalogo.Tipo.DESCRIPCION)).isNull();
        assertThat(TextoCatalogo.normalizar(" \t\n ", TextoCatalogo.Tipo.DESCRIPCION)).isNull();
    }
}
