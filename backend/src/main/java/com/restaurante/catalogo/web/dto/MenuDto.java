package com.restaurante.catalogo.web.dto;

import java.util.List;

/**
 * Menú público expuesto sin autenticación para el QR y la web del cliente.
 */
public record MenuDto(
        InfoRestauranteDto restaurante,
        List<CategoriaMenuDto> categorias) {
}