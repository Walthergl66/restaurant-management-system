package com.restaurante.catalogo.web.dto;

/**
 * Datos generales del restaurante para el menú público.
 */
public record InfoRestauranteDto(
        String nombre,
        String moneda) {
}