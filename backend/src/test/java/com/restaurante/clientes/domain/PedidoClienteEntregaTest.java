package com.restaurante.clientes.domain;

import com.restaurante.shared.domain.Money;
import com.restaurante.shared.domain.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Entrega en el mostrador: solo los pedidos de retiro; el domicilio lo entrega el repartidor. */
class PedidoClienteEntregaTest {

    private PedidoCliente listo(String metodoEntrega) {
        PedidoCliente pedido = new PedidoCliente("APP-1", 1L, "EFECTIVO", metodoEntrega,
                "DOMICILIO".equals(metodoEntrega) ? 5L : null, "idem-1");
        pedido.agregarLinea(new PedidoClienteLinea(10L, "Hamburguesa", Money.of("5.00"), 1, null));
        pedido.confirmar("idem-1", Money.ZERO);
        pedido.marcarEnPreparacion();
        pedido.marcarListo();
        return pedido;
    }

    @Test
    void unRetiroListoPasaAEntregado() {
        PedidoCliente pedido = listo("RETIRAR");
        pedido.entregarEnMostrador();
        assertEquals(EstadoPedidoCliente.ENTREGADO, pedido.getEstado());
    }

    @Test
    void entregarDosVecesNoCambiaNadaNiFalla() {
        PedidoCliente pedido = listo("RETIRAR");
        pedido.entregarEnMostrador();
        pedido.entregarEnMostrador();
        assertEquals(EstadoPedidoCliente.ENTREGADO, pedido.getEstado());
    }

    @Test
    void unRetiroQueAunNoEstaListoSeRechaza() {
        PedidoCliente pedido = new PedidoCliente("APP-2", 1L, "EFECTIVO", "RETIRAR", null, "idem-2");
        pedido.agregarLinea(new PedidoClienteLinea(10L, "Hamburguesa", Money.of("5.00"), 1, null));
        pedido.confirmar("idem-2", Money.ZERO);

        assertThrows(BusinessRuleException.class, pedido::entregarEnMostrador);
        assertEquals(EstadoPedidoCliente.CONFIRMADO, pedido.getEstado());
    }

    @Test
    void unDomicilioNoSeEntregaEnElMostrador() {
        PedidoCliente pedido = listo("DOMICILIO");

        BusinessRuleException error = assertThrows(BusinessRuleException.class, pedido::entregarEnMostrador);
        assertEquals("El domicilio lo entrega el repartidor", error.getMessage());
        assertEquals(EstadoPedidoCliente.LISTO, pedido.getEstado());
    }
}
