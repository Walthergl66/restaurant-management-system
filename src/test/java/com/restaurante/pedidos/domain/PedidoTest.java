package com.restaurante.pedidos.domain;

import com.restaurante.shared.domain.Money;
import com.restaurante.shared.domain.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Reglas de negocio del pedido: ciclo de estados, bloqueo al confirmar y
 * cálculo de totales desde la línea.
 */
class PedidoTest {

    @Test
    void creaBorradorYCalculaTotalesConExtras() {
        Pedido pedido = new Pedido("C-001", 1L, null);
        pedido.agregarLinea(
                10L, "Encebollado", Money.of("5.00"), 2,
                List.of(new ExtraLinea(9L, "Tocino", Money.of("1.00"))),
                List.of(new IngredienteRemovido("cebolla")),
                null);

        PedidoLinea linea = pedido.getLineas().get(0);
        assertEquals(EstadoPedido.BORRADOR, pedido.getEstado());
        assertEquals(1, linea.getIngredientesRemovidos().size());
        assertEquals("cebolla", linea.getIngredientesRemovidos().get(0).getNombre());
        // 5.00 * 2 + 1.00 * 2
        assertEquals("12.00", linea.subtotal().getAmount().toPlainString());
        assertEquals("12.00", pedido.totalLineas().getAmount().toPlainString());
    }

    @Test
    void confirmarBloqueaElBorrador() {
        Pedido pedido = new Pedido("C-002", 1L, null);
        PedidoLinea linea = pedido.agregarLinea(
                10L, "Producto", Money.of("5.00"), 1, List.of(), List.of(), null);
        ReflectionTestUtils.setField(linea, "id", 1L);

        pedido.confirmar();

        assertEquals(EstadoPedido.CONFIRMADO, pedido.getEstado());
        assertThrows(BusinessRuleException.class,
                () -> pedido.agregarLinea(11L, "Otro", Money.of("2.00"), 1, List.of(), List.of(), null));
        assertThrows(BusinessRuleException.class, () -> pedido.quitarLinea(1L));
        assertThrows(BusinessRuleException.class,
                () -> pedido.actualizarLinea(1L, 3, List.of(), List.of(), null));
    }

    @Test
    void noConfirmarSinLineas() {
        Pedido pedido = new Pedido("C-003", 1L, null);
        assertThrows(BusinessRuleException.class, pedido::confirmar);
    }

    @Test
    void transicionesValidasHastaEntregar() {
        Pedido pedido = new Pedido("C-004", 1L, null);
        pedido.agregarLinea(10L, "Producto", Money.of("5.00"), 1, List.of(), List.of(), null);

        pedido.confirmar();
        pedido.marcarEnPreparacion();
        pedido.marcarListo();
        pedido.entregar();

        assertEquals(EstadoPedido.ENTREGADO, pedido.getEstado());
    }

    @Test
    void rechazaTransicionesFueraDeOrden() {
        Pedido pedido = new Pedido("C-005", 1L, null);
        pedido.agregarLinea(10L, "Producto", Money.of("5.00"), 1, List.of(), List.of(), null);

        assertThrows(BusinessRuleException.class, pedido::marcarListo);
        pedido.confirmar();
        assertThrows(BusinessRuleException.class, pedido::entregar);
        assertThrows(BusinessRuleException.class, pedido::marcarListo);
        pedido.marcarEnPreparacion();
        assertThrows(BusinessRuleException.class, pedido::confirmar);
    }

    @Test
    void confirmaIdempotenteConSuClave() {
        Pedido pedido = new Pedido("C-006", 1L, null);
        pedido.agregarLinea(10L, "Producto", Money.of("5.00"), 1, List.of(), List.of(), null);

        pedido.confirmar();
        pedido.agregarConfirmacion("key-ABC");
        assertTrue(pedido.tieneConfirmacionConClave("key-ABC"));
    }
}