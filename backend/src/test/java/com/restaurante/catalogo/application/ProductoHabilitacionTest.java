package com.restaurante.catalogo.application;

import com.restaurante.catalogo.domain.Producto;
import com.restaurante.catalogo.infrastructure.*;
import com.restaurante.catalogo.web.dto.ProductoRequest;
import com.restaurante.catalogo.web.dto.HabilitacionCatalogoRequest;
import com.restaurante.shared.domain.Money;
import com.restaurante.shared.domain.exception.ConflictException;
import com.restaurante.shared.domain.exception.NotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProductoHabilitacionTest {
    private final ProductoRepository repository = mock(ProductoRepository.class);
    private final ProductoService service = new ProductoService(repository, mock(CategoriaRepository.class), mock(AreaRepository.class), mock(ExtraRepository.class), mock(com.restaurante.archivos.Archivos.class));

    private ProductoRequest solicitud(String nombre) {
        return new ProductoRequest(nombre, null, null, new java.math.BigDecimal("1.00"), null, null, java.util.Set.of(), java.util.List.of());
    }

    private Producto existente() {
        Producto registro = new Producto("Cocina", Money.of("1.00"));
        ReflectionTestUtils.setField(registro, "id", 7L);
        when(repository.findById(7L)).thenReturn(Optional.of(registro));
        return registro;
    }

    @Test
    void crearRechazaNombreExistenteIncluyendoInactivos() {
        when(repository.existsByNombreIgnoreCase("COCINA")).thenReturn(true);
        assertThatThrownBy(() -> service.crear(solicitud("  COCINA  ")))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("reactiva");
        verify(repository, never()).save(any());
    }

    @Test
    void editarRechazaNombreDeOtroRegistroSinModificarElOriginal() {
        Producto registro = existente();
        when(repository.existsByNombreIgnoreCaseAndIdNot("COCINA", 7L)).thenReturn(true);
        assertThatThrownBy(() -> service.actualizar(7L, solicitud(" COCINA ")))
                .isInstanceOf(ConflictException.class);
        assertThat(registro.getNombre()).isEqualTo("Cocina");
    }

    @Test
    void editarPermiteConservarSuPropioNombre() {
        existente();
        var respuesta = service.actualizar(7L, solicitud(" Cocina "));
        assertThat(respuesta.id()).isEqualTo(7L);
        assertThat(respuesta.nombre()).isEqualTo("Cocina");
        verify(repository).existsByNombreIgnoreCaseAndIdNot("Cocina", 7L);
    }

    @Test
    void reactivarConservaIdYNoInsertaOtroRegistro() {
        Producto registro = existente();
        registro.desactivar();
        var respuesta = service.cambiarHabilitacion(7L, new HabilitacionCatalogoRequest(true));
        assertThat(respuesta.id()).isEqualTo(7L);
        assertThat(respuesta.activo()).isTrue();
        assertThat(registro.isActivo()).isTrue();
        verify(repository).existsByNombreIgnoreCaseAndIdNot("Cocina", 7L);
        verify(repository, never()).save(any());
    }

    @Test
    void reactivarRechazaDuplicadoHistorico() {
        Producto registro = existente();
        registro.desactivar();
        when(repository.existsByNombreIgnoreCaseAndIdNot("Cocina", 7L)).thenReturn(true);
        assertThatThrownBy(() -> service.cambiarHabilitacion(7L, new HabilitacionCatalogoRequest(true)))
                .isInstanceOf(ConflictException.class);
        assertThat(registro.isActivo()).isFalse();
    }

    @Test
    void desactivarEsIdempotenteYPermiteCorregirDuplicadosHistoricos() {
        Producto registro = existente();
        service.cambiarHabilitacion(7L, new HabilitacionCatalogoRequest(false));
        var respuesta = service.cambiarHabilitacion(7L, new HabilitacionCatalogoRequest(false));
        assertThat(respuesta.id()).isEqualTo(7L);
        assertThat(registro.isActivo()).isFalse();
        verify(repository, never()).existsByNombreIgnoreCaseAndIdNot(anyString(), anyLong());
        verify(repository, never()).delete(any());
    }

    @Test
    void idInexistenteNoCreaRegistro() {
        assertThatThrownBy(() -> service.cambiarHabilitacion(99L, new HabilitacionCatalogoRequest(true)))
                .isInstanceOf(NotFoundException.class);
        verify(repository, never()).save(any());
    }
}

