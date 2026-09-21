package com.restaurante.comandas.domain;

/**
 * Tipo de comanda. Una ORDEN nace al confirmar un pedido (vida normal en
 * cocina); una CANCELACION es la comanda impresa cuando se aprueba una
 * anulación, para avisar al área que ya no se prepara esa línea.
 */
public enum TipoComanda {
    ORDEN,
    CANCELACION
}