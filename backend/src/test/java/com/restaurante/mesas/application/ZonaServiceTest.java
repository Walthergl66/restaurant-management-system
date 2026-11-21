package com.restaurante.mesas.application;

import com.restaurante.mesas.domain.Zona;
import com.restaurante.mesas.infrastructure.MesaRepository;
import com.restaurante.mesas.infrastructure.ZonaRepository;
import com.restaurante.shared.domain.exception.BusinessRuleException;
import com.restaurante.shared.domain.exception.ConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Reglas de la administración de zonas, sin base de datos. */
class ZonaServiceTest {

    private ZonaRepository zonas;
    private MesaRepository mesas;
    private ZonaService servicio;
    private final List<Zona> guardadas = new ArrayList<>();

    private Zona zona(long id, String nombre, int orden) {
        Zona zona = new Zona(nombre, orden);
        ReflectionTestUtils.setField(zona, "id", id);
        return zona;
    }

    @BeforeEach
    void preparar() {
        zonas = mock(ZonaRepository.class);
        mesas = mock(MesaRepository.class);
        servicio = new ZonaService(zonas, mesas);
        guardadas.clear();
        guardadas.add(zona(1, "Salón", 1));
        guardadas.add(zona(2, "Terraza", 2));
        when(zonas.findAll()).thenAnswer(i -> List.copyOf(guardadas));
        when(zonas.findAllByOrderByOrdenAscIdAsc()).thenAnswer(i -> List.copyOf(guardadas));
        when(zonas.count()).thenAnswer(i -> (long) guardadas.size());
        when(zonas.findById(any())).thenAnswer(i -> guardadas.stream().filter(z -> z.getId().equals(i.getArgument(0))).findFirst());
        when(zonas.save(any(Zona.class))).thenAnswer(i -> {
            Zona nueva = i.getArgument(0);
            ReflectionTestUtils.setField(nueva, "id", 10L + guardadas.size());
            guardadas.add(nueva);
            return nueva;
        });
    }

    @Test
    void crearLaPoneAlFinalDelOrden() {
        ZonaVista creada = servicio.crear("  patio trasero ");
        assertEquals("Patio trasero", creada.nombre());
        assertEquals(3, creada.orden());
    }

    @Test
    void unNombreRepetidoSinImportarMayusculasNiTildesEsConflicto() {
        assertThrows(ConflictException.class, () -> servicio.crear("terraza"));
        assertThrows(ConflictException.class, () -> servicio.crear("SALON"));
    }

    @Test
    void renombrarConElNombreDeOtraZonaEsConflictoPeroConElPropioNo() {
        assertThrows(ConflictException.class, () -> servicio.renombrar(2L, "salón"));
        assertEquals("TERRAZA", servicio.renombrar(2L, "TERRAZA").nombre(), "cambiar solo las mayúsculas del propio nombre es válido");
    }

    @Test
    void reordenarExigeTodasLasZonasUnaSolaVez() {
        assertThrows(BusinessRuleException.class, () -> servicio.reordenar(List.of(1L)));
        assertThrows(BusinessRuleException.class, () -> servicio.reordenar(List.of(1L, 1L)));
        assertThrows(BusinessRuleException.class, () -> servicio.reordenar(List.of(1L, 99L)));
        assertThrows(BusinessRuleException.class, () -> servicio.reordenar(null));
        assertEquals(1, guardadas.get(0).getOrden(), "el orden no queda a medias");
    }

    @Test
    void reordenarGuardaElNuevoOrden() {
        servicio.reordenar(List.of(2L, 1L));
        assertEquals(2, guardadas.get(0).getOrden());
        assertEquals(1, guardadas.get(1).getOrden());
    }

    @Test
    void noSeEliminaUnaZonaConMesas() {
        when(mesas.countByZonaId(2L)).thenReturn(3L);
        assertThrows(BusinessRuleException.class, () -> servicio.eliminar(2L));
        verify(zonas, never()).delete(any(Zona.class));
    }

    @Test
    void noSeEliminaLaUltimaZona() {
        guardadas.remove(1);
        when(mesas.countByZonaId(1L)).thenReturn(0L);
        assertThrows(BusinessRuleException.class, () -> servicio.eliminar(1L));
        verify(zonas, never()).delete(any(Zona.class));
    }

    @Test
    void eliminarUnaZonaVaciaLaBorraYDejaConsecutivoElOrden() {
        guardadas.add(zona(3, "Patio", 3));
        when(mesas.countByZonaId(2L)).thenReturn(0L);
        servicio.eliminar(2L);
        verify(zonas).delete(guardadas.get(1));
        Zona patio = guardadas.stream().filter(z -> z.getId() == 3L).findFirst().orElseThrow();
        assertEquals(2, patio.getOrden(), "Patio sube de la posición 3 a la 2");
    }

    @Test
    void laPrimeraZonaEsLaDeMenorOrden() {
        assertEquals("Salón", servicio.primera().getNombre());
    }

    @Test
    void cargarUnaZonaInexistenteFalla() {
        when(zonas.findById(99L)).thenReturn(Optional.empty());
        assertThrows(com.restaurante.shared.domain.exception.NotFoundException.class, () -> servicio.cargar(99L));
    }
}
