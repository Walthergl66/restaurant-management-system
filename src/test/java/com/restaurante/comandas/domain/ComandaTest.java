package com.restaurante.comandas.domain;

import com.restaurante.shared.domain.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ComandaTest {

    private Comanda comanda() {
        return new Comanda("PED-1", 25, 3L, "Cocina");
    }

    @Test
    void nacePendiente() {
        assertEquals(ComandaEstado.PENDIENTE, comanda().getEstado());
    }

    @Test
    void transitaPendienteAPreparacionAListo() {
        Comanda c = comanda();
        c.marcarEnPreparacion();
        assertEquals(ComandaEstado.EN_PREPARACION, c.getEstado());
        c.marcarListo();
        assertEquals(ComandaEstado.LISTO, c.getEstado());
    }

    @Test
    void repetirMarcacionesEsIdempotente() {
        Comanda c = comanda();
        c.marcarEnPreparacion();
        c.marcarEnPreparacion();
        assertEquals(ComandaEstado.EN_PREPARACION, c.getEstado());
        c.marcarListo();
        c.marcarListo();
        assertEquals(ComandaEstado.LISTO, c.getEstado());
    }

    @Test
    void listaDirectaSeRechaza() {
        Comanda c = comanda();
        assertThrows(BusinessRuleException.class, c::marcarListo);
        assertEquals(ComandaEstado.PENDIENTE, c.getEstado());
    }

    @Test
    void noVuelveATrasDesdeListo() {
        Comanda c = comanda();
        c.marcarEnPreparacion();
        c.marcarListo();
        assertThrows(BusinessRuleException.class, c::marcarEnPreparacion);
        assertEquals(ComandaEstado.LISTO, c.getEstado());
    }

    @Test
    void lineaSeEnlazaConSuComanda() {
        Comanda c = comanda();
        c.agregarLinea(new ComandaLinea(9L, "Lomo", 2, null, null, "poco hecho", 1));
        assertEquals(1, c.getLineas().size());
        assertEquals("Lomo", c.getLineas().get(0).getNombreProducto());
        assertTrue(c.getLineas().get(0).getCantidad() == 2);
    }
}