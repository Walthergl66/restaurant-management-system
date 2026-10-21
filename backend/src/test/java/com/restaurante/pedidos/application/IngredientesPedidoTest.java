package com.restaurante.pedidos.application;

import com.restaurante.catalogo.Catalogo;
import com.restaurante.catalogo.ProductoParaPedido;
import com.restaurante.mesas.Mesas;
import com.restaurante.pedidos.PedidoConfirmado;
import com.restaurante.pedidos.domain.Pedido;
import com.restaurante.pedidos.infrastructure.PedidoRepository;
import com.restaurante.pedidos.web.dto.AgregarLineaRequest;
import com.restaurante.pedidos.web.dto.ActualizarLineaRequest;
import com.restaurante.shared.domain.Money;
import com.restaurante.shared.domain.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class IngredientesPedidoTest {
    private final PedidoRepository repository = mock(PedidoRepository.class);
    private final Catalogo catalogo = mock(Catalogo.class);
    private final ApplicationEventPublisher eventos = mock(ApplicationEventPublisher.class);
    private final PedidoService service = new PedidoService(repository, catalogo, mock(Mesas.class), eventos);
    private final Pedido pedido = new Pedido("PRUEBA-UNITARIA", 1L, null);

    IngredientesPedidoTest() {
        when(repository.findByCodigoConRelaciones(pedido.getCodigo())).thenReturn(Optional.of(pedido));
        when(catalogo.productoParaPedido(2L)).thenReturn(Optional.of(producto(List.of("Cebolla"))));
    }

    private ProductoParaPedido producto(List<String> ingredientes) {
        return new ProductoParaPedido(2L, "Hamburguesa", Money.of("3.75"), 1L, "Cocina", true, ingredientes);
    }

    private void agregar(List<String> ingredientes) {
        service.agregarLinea(pedido.getCodigo(), new AgregarLineaRequest(2L, 1, List.of(), ingredientes, null));
    }

    @Test void transmiteLaPersonalizacionAlEventoDeCocinaSinCambiarPrecio() {
        agregar(List.of("Cebolla"));
        service.confirmar(pedido.getCodigo(), "clave-prueba");
        var captor = org.mockito.ArgumentCaptor.forClass(PedidoConfirmado.class);
        verify(eventos).publishEvent(captor.capture());
        assertEquals(List.of("Cebolla"), captor.getValue().lineas().getFirst().ingredientesRemovidos());
        assertEquals(Money.of("3.75"), pedido.totalLineas());
    }

    @Test void rechazaIngredientesAjenosAlAgregar() {
        assertThrows(BusinessRuleException.class, () -> agregar(List.of("Tomate")));
        assertTrue(pedido.getLineas().isEmpty());
    }

    @Test void rechazaIngredientesAjenosAlEditarSinModificarLaLinea() {
        agregar(List.of("Cebolla"));
        ReflectionTestUtils.setField(pedido.getLineas().getFirst(), "id", 10L);
        assertThrows(BusinessRuleException.class, () -> service.actualizarLinea(pedido.getCodigo(), 10L,
                new ActualizarLineaRequest(2, List.of(), List.of("Tomate"), null)));
        assertEquals(1, pedido.getLineas().getFirst().getCantidad());
    }

    @Test void revalidaIngredientesAntesDeConfirmar() {
        agregar(List.of("Cebolla"));
        when(catalogo.productoParaPedido(2L)).thenReturn(Optional.of(producto(List.of())));
        assertThrows(BusinessRuleException.class, () -> service.confirmar(pedido.getCodigo(), "clave-prueba"));
        verifyNoInteractions(eventos);
    }
}
