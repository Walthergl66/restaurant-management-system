package com.restaurante.catalogo.application;

import com.restaurante.catalogo.domain.Categoria;
import com.restaurante.catalogo.infrastructure.CategoriaRepository;
import com.restaurante.catalogo.web.dto.CategoriaRequest;
import com.restaurante.shared.domain.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static com.restaurante.catalogo.application.OrdenCategoriasCommand.Modo.*;

class CategoriaOrdenTest {
    private final CategoriaRepository repository = mock(CategoriaRepository.class);
    private final CategoriaService service = new CategoriaService(repository);

    private Categoria categoria(long id, String nombre, int orden) {
        Categoria categoria = new Categoria(nombre, orden);
        ReflectionTestUtils.setField(categoria, "id", id);
        return categoria;
    }

    @Test
    void alfabeticoOrdenaEnEspanolIgnorandoMayusculasYTildes() {
        var bebidas = categoria(1, "bebidas", 0);
        var areas = categoria(2, "Áreas", 0);
        var nachos = categoria(3, "Nachos", 0);
        when(repository.findAll()).thenReturn(List.of(nachos, bebidas, areas));
        assertThat(service.listar(false)).extracting(r -> r.nombre()).containsExactly("Áreas", "Bebidas", "Nachos");
    }

    @Test
    void guardaPersonalizadoIncluyendoInactivosYPermiteVolverAAlfabetico() {
        var bebidas = categoria(1, "Bebidas", 0);
        var hamburguesas = categoria(2, "Hamburguesas", 0);
        bebidas.desactivar();
        when(repository.findAll()).thenReturn(List.of(bebidas, hamburguesas));
        assertThat(service.cambiarOrden(new OrdenCategoriasCommand(PERSONALIZADO, List.of(2L, 1L))))
                .extracting(r -> r.id()).containsExactly(2L, 1L);
        assertThat(bebidas.getOrden()).isEqualTo(2);
        assertThat(service.cambiarOrden(new OrdenCategoriasCommand(ALFABETICO, List.of())))
                .extracting(r -> r.id()).containsExactly(1L, 2L);
        assertThat(bebidas.getOrden()).isZero();
        assertThat(hamburguesas.getOrden()).isZero();
    }

    @Test
    void rechazaListaIncompletaODuplicadaAntesDeModificar() {
        var a = categoria(1, "A", 1);
        var b = categoria(2, "B", 2);
        when(repository.findAll()).thenReturn(List.of(a, b));
        for (var ids : List.of(List.of(1L), List.of(1L, 1L), List.of(1L, 99L))) {
            assertThatThrownBy(() -> service.cambiarOrden(new OrdenCategoriasCommand(PERSONALIZADO, ids)))
                    .isInstanceOf(BusinessRuleException.class);
        }
        assertThat(a.getOrden()).isEqualTo(1);
        assertThat(b.getOrden()).isEqualTo(2);
    }

    @Test
    void categoriaNuevaVaAlFinalEnPersonalizadoYCeroEnAlfabetico() {
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(repository.findAll()).thenReturn(List.of(categoria(1, "A", 4)));
        assertThat(service.crear(new CategoriaRequest("B", null, null)).orden()).isEqualTo(5);
        when(repository.findAll()).thenReturn(List.of(categoria(1, "A", 0)));
        assertThat(service.crear(new CategoriaRequest("C", null, null)).orden()).isZero();
    }

    @Test
    void editarNombreNoAlteraPosicionPersonalizada() {
        var a = categoria(1, "A", 3);
        when(repository.findById(1L)).thenReturn(Optional.of(a));
        assertThat(service.actualizar(1L, new CategoriaRequest("Z", null, 0)).orden()).isEqualTo(3);
    }
}
