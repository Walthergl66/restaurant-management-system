package com.restaurante.mesas.domain;

import com.restaurante.shared.domain.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ZonaTest {

    @Test
    void elNombreSeRecortaYSeCompactanLosEspacios() {
        assertEquals("Terraza", new Zona("  Terraza  ", 1).getNombre());
        assertEquals("Patio trasero", new Zona("Patio    trasero", 1).getNombre());
    }

    @Test
    void laPrimeraLetraSeEscribeEnMayuscula() {
        assertEquals("Terraza", new Zona("terraza", 1).getNombre());
        assertEquals("Álamo", new Zona("álamo", 1).getNombre());
    }

    @Test
    void unNombreVacioOEnBlancoSeRechaza() {
        assertThrows(BusinessRuleException.class, () -> new Zona("", 1));
        assertThrows(BusinessRuleException.class, () -> new Zona("   ", 1));
        assertThrows(BusinessRuleException.class, () -> new Zona(null, 1));
    }

    @Test
    void elNombreAdmiteSesentaCaracteresPeroNoSesentaYUno() {
        assertEquals(60, new Zona("a".repeat(60), 1).getNombre().length());
        assertThrows(BusinessRuleException.class, () -> new Zona("a".repeat(61), 1));
    }

    @Test
    void renombrarAplicaLasMismasReglas() {
        Zona zona = new Zona("Salón", 1);
        zona.cambiarNombre("  patio ");
        assertEquals("Patio", zona.getNombre());
        assertThrows(BusinessRuleException.class, () -> zona.cambiarNombre("  "));
        assertEquals("Patio", zona.getNombre());
    }

    @Test
    void elOrdenDebeSerPositivo() {
        assertThrows(BusinessRuleException.class, () -> new Zona("Salón", 0));
        Zona zona = new Zona("Salón", 2);
        assertThrows(BusinessRuleException.class, () -> zona.cambiarOrden(0));
        zona.cambiarOrden(5);
        assertEquals(5, zona.getOrden());
    }

    @Test
    void laClaveIgnoraMayusculasEspaciosYTildes() {
        assertEquals(Zona.clave("Terraza"), Zona.clave("  terraza "));
        assertEquals(Zona.clave("Salón"), Zona.clave("SALON"));
        assertEquals(Zona.clave("Patio trasero"), Zona.clave("patio   TRASERO"));
    }

    @Test
    void laClaveConservaLaEnie() {
        assertNotEquals(Zona.clave("Año"), Zona.clave("Ano"));
    }
}
