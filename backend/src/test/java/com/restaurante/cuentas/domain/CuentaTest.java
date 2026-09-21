package com.restaurante.cuentas.domain;

import com.restaurante.shared.domain.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Transiciones de la cuenta: ABIERTA → CERRADA en una sola clase; un estado ya
 * cerrado no admite otro cierre.
 */
class CuentaTest {

    @Test
    void abrePorDefectoYSeCierraUnaVez() {
        Cuenta cuenta = new Cuenta(5L);

        assertEquals(EstadoCuenta.ABIERTA, cuenta.getEstado());
        assertEquals(5L, cuenta.getMesaId());

        cuenta.cerrar();
        assertEquals(EstadoCuenta.CERRADA, cuenta.getEstado());
    }

    @Test
    void cerrarUnaCuentaCerradaLanza() {
        Cuenta cuenta = new Cuenta(5L);
        cuenta.cerrar();

        assertThrows(BusinessRuleException.class, cuenta::cerrar);
    }
}