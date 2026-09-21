package com.restaurante.anulaciones.domain;

import com.restaurante.shared.domain.Money;
import com.restaurante.shared.domain.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Regla de negocio de anulaciones (RF-20 a RF-23): la anulación es un registro
 * aparte (la línea original nunca se toca), congela datos al solicitarse y solo
 * una solicitud pendiente admite resolución.
 */
class AnulacionTest {

    private Anulacion nueva() {
        return new Anulacion("P-1", 1L, 10L, "Ceviche",
                Money.of("10.00"), 1, "lo pidió con limón", 1L, "Cocina", "mesero1");
    }

    @Test
    void congelaDatosAlSolicitar() {
        Anulacion anulacion = nueva();

        assertEquals(EstadoAnulacion.SOLICITADA, anulacion.getEstado());
        assertEquals("Ceviche", anulacion.getNombreProducto());
        assertEquals("10.00", anulacion.getPrecioUnitario().getAmount().toPlainString());
        assertEquals(1, anulacion.getCantidad());
        assertEquals(1L, anulacion.getLineaId());
    }

    @Test
    void aprobarDescribeRegistroYResuelve() {
        Anulacion anulacion = nueva();

        anulacion.aprobar("cajero1");

        assertEquals(EstadoAnulacion.APROBADA, anulacion.getEstado());
        assertEquals("cajero1", anulacion.getResueltoPor());
        assertNotNull(anulacion.getResueltaAt());
    }

    @Test
    void rechazarNoDesechaElRegistro() {
        Anulacion anulacion = nueva();

        anulacion.rechazar("cajero1");

        assertEquals(EstadoAnulacion.RECHAZADA, anulacion.getEstado());
        assertNotNull(anulacion.getResueltaAt());
    }

    @Test
    void resolverUnaYaResueltaLanza() {
        Anulacion anulacion = nueva();
        anulacion.aprobar("cajero1");

        assertThrows(BusinessRuleException.class, () -> anulacion.rechazar("cajero1"));
        assertThrows(BusinessRuleException.class, () -> anulacion.aprobar("cajero1"));
    }

    @Test
    void cantidadInvalidaExplotaEnLaCuna() {
        assertThrows(BusinessRuleException.class, () -> {
            new Anulacion("P-1", 1L, 10L, "Ceviche",
                    Money.of("10.00"), 0, "motivo", 1L, "Cocina", "mesero1");
        });
    }
}