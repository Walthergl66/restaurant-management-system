package com.restaurante.anulaciones;

import java.util.List;

/**
 * API pública del módulo de anulaciones. La consume la cuenta para descontar
 * las anulaciones aprobadas del total (RNF-16).
 */
public interface Anulaciones {

    /**
     * Anulaciones APROBADAS de los pedidos indicados, para el cálculo de cuenta.
     */
    List<AnulacionResumen> aprobadasDe(List<String> pedidoCodigos);
}