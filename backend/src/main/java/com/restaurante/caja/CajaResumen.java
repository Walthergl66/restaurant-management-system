package com.restaurante.caja;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Vista de una caja para otros módulos y para la API.
 */
public record CajaResumen(
        Long id,
        String estado,
        BigDecimal aperturaInicial,
        BigDecimal cierreEsperado,
        BigDecimal cierreReal,
        BigDecimal diferencia,
        String abiertaPor,
        String cerradaPor,
        Instant abiertaAt,
        Instant cerradaAt) {
}