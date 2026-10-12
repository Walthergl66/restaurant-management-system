package com.restaurante.catalogo.application;

import com.restaurante.catalogo.domain.*;
import com.restaurante.shared.domain.Money;
import com.restaurante.shared.domain.exception.BusinessRuleException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.restaurante.catalogo.infrastructure.CategoriaRepository;
import com.restaurante.catalogo.web.dto.CategoriaRequest;
import com.restaurante.shared.domain.exception.ConflictException;
import org.junit.jupiter.api.Test;

class NombreCatalogoTest {
    @Test
    void validaDuplicadosConElMismoNombreNormalizadoQueSeGuarda() {
        CategoriaRepository repository = mock(CategoriaRepository.class);
        when(repository.existsByNombreIgnoreCase("Salsa BBQ")).thenReturn(true);
        assertThatThrownBy(() -> new CategoriaService(repository)
                .crear(new CategoriaRequest("  salsa   BBQ  ", null, null)))
                .isInstanceOf(ConflictException.class);
        verify(repository).existsByNombreIgnoreCase("Salsa BBQ");
        verify(repository, never()).save(any());
    }
    @ParameterizedTest
    @CsvSource(value = {
            "hamburguesas|Hamburguesas",
            "  bebidas   frías  |Bebidas frías",
            "BBQ|BBQ", "Coca-Cola|Coca-Cola",
            "hamburguesa XL|Hamburguesa XL",
            "bbq|Bbq", "ñame|Ñame", "área de BBQ|Área de BBQ"
    }, delimiter = '|', ignoreLeadingAndTrailingWhitespace = false)
    void todosLosNombresDelCatalogoUsanLaMismaRegla(String entrada, String esperado) {
        assertThat(NombreCatalogo.normalizar(entrada)).isEqualTo(esperado);
        assertThat(NombreCatalogo.normalizar(esperado)).isEqualTo(esperado);
        assertThat(new Categoria(entrada, 0).getNombre()).isEqualTo(esperado);
        assertThat(new Area(entrada).getNombre()).isEqualTo(esperado);
        assertThat(new Extra(entrada, Money.ZERO).getNombre()).isEqualTo(esperado);
        Producto producto = new Producto(entrada, Money.ZERO);
        assertThat(producto.getNombre()).isEqualTo(esperado);
        assertThat(new IngredienteRemovible(producto, entrada).getNombre()).isEqualTo(esperado);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t\n", "\u00a0"})
    void rechazaNombresVacios(String entrada) {
        assertThatThrownBy(() -> NombreCatalogo.normalizar(entrada)).isInstanceOf(BusinessRuleException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"  salsa\t BBQ  ", "salsa\u00a0\u00a0BBQ", "salsa\nBBQ"})
    void normalizaEspaciosPegadosDesdeOtrosMedios(String entrada) {
        assertThat(NombreCatalogo.normalizar(entrada)).isEqualTo("Salsa BBQ");
    }
}
