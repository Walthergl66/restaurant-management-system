package com.restaurante.caja.domain;

import com.restaurante.shared.domain.Money;
import com.restaurante.shared.domain.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Transiciones de caja (RF-32 a RF-35): apertura inicial, cierre conciliado
 * con diferencia calculada en una sola clase y guardia de una sola vez.
 */
class CajaTest {

    @Test
    void abreConMontoInicial() {
        Caja caja = new Caja(Money.of("10.00"), "cajero1");

        assertEquals(EstadoCaja.ABIERTA, caja.getEstado());
        assertEquals("10.00", caja.getAperturaInicial().getAmount().toPlainString());
        assertEquals("cajero1", caja.getAbiertaPor());
        assertNull(caja.getCierreReal());
    }

    @Test
    void cerrarCalculaLaDiferencia() {
        Caja caja = new Caja(Money.of("10.00"), "cajero1");

        caja.cerrar(Money.of("120.00"), Money.of("115.00"), "cajero1");

        assertEquals(EstadoCaja.CERRADA, caja.getEstado());
        assertEquals("115.00", caja.getCierreEsperado().getAmount().toPlainString());
        assertEquals("120.00", caja.getCierreReal().getAmount().toPlainString());
        assertEquals("5.00", caja.getDiferencia().getAmount().toPlainString());
        assertEquals("cajero1", caja.getCerradaPor());
    }

    @Test
    void faltanteDaDiferenciaNegativa() {
        Caja caja = new Caja(Money.ZERO, "cajero1");
        caja.cerrar(Money.of("1.50"), Money.of("2.00"), "cajero1");
        assertEquals("-0.50", caja.getDiferencia().getAmount().toPlainString());
    }

    @Test
    void cerrarUnaCajaCerradaLanza() {
        Caja caja = new Caja(Money.ZERO, "cajero1");
        caja.cerrar(Money.ZERO, Money.ZERO, "cajero1");

        assertThrows(BusinessRuleException.class,
                () -> caja.cerrar(Money.ZERO, Money.ZERO, "cajero1"));
    }
}