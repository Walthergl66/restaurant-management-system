package com.restaurante.mesas.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** El estado de la mesa (libre u ocupada) lo mueve la cuenta; el administrador solo la habilita o deshabilita. */
class MesaTest {

    private Mesa mesa() {
        return new Mesa(1, 4, new Zona("Salón", 1));
    }

    @Test
    void naceLibre() {
        assertEquals(EstadoMesa.LIBRE, mesa().getEstado());
    }

    @Test
    void ocuparLaPasaAOcupadaYRepetirNoCambiaNada() {
        Mesa mesa = mesa();
        mesa.ocupar();
        mesa.ocupar();
        assertEquals(EstadoMesa.OCUPADA, mesa.getEstado());
    }

    @Test
    void liberarLaPasaALibreYRepetirNoCambiaNada() {
        Mesa mesa = mesa();
        mesa.ocupar();
        mesa.liberar();
        mesa.liberar();
        assertEquals(EstadoMesa.LIBRE, mesa.getEstado());
    }

    @Test
    void liberarUnaMesaLibreLaDejaLibre() {
        Mesa mesa = mesa();
        mesa.liberar();
        assertEquals(EstadoMesa.LIBRE, mesa.getEstado());
    }

    @Test
    void deshabilitarOHabilitarNoCambiaElEstado() {
        Mesa mesa = mesa();
        mesa.ocupar();
        mesa.deshabilitar();
        assertEquals(EstadoMesa.OCUPADA, mesa.getEstado());
        mesa.habilitar();
        assertEquals(EstadoMesa.OCUPADA, mesa.getEstado());
    }
}
