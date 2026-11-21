package com.restaurante.comandas.application;

import com.restaurante.clientes.Clientes;
import com.restaurante.comandas.domain.Comanda;
import com.restaurante.comandas.infrastructure.ComandaRepository;
import com.restaurante.comandas.web.dto.ComandaResponse;
import com.restaurante.mesas.MesaResumen;
import com.restaurante.mesas.Mesas;
import com.restaurante.mesas.domain.EstadoMesa;
import com.restaurante.pedidos.PedidoResumen;
import com.restaurante.pedidos.Pedidos;
import com.restaurante.shared.outbox.OutboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** De dónde viene una comanda: mesa del salón o pedido de la app (retiro / domicilio). */
class ComandaServiceOrigenTest {

    private ComandaRepository comandas;
    private Pedidos pedidos;
    private Clientes clientes;
    private Mesas mesas;
    private ComandaService servicio;

    @BeforeEach
    void preparar() {
        comandas = mock(ComandaRepository.class);
        pedidos = mock(Pedidos.class);
        clientes = mock(Clientes.class);
        mesas = mock(Mesas.class);
        servicio = new ComandaService(comandas, mock(OutboxRepository.class), pedidos, clientes, mesas);
    }

    private ComandaResponse unicaComandaDe(String pedidoCodigo) {
        when(comandas.buscarConLineas(null, null))
                .thenReturn(List.of(new Comanda(pedidoCodigo, 1, 3L, "Cocina")));
        return servicio.listar(null, null).get(0);
    }

    @Test
    void laComandaDeSalonTraeLaMesaYNoLaFormaDeEntrega() {
        when(pedidos.pedidoConfirmado("PED-1"))
                .thenReturn(Optional.of(new PedidoResumen("PED-1", 5L, "CONFIRMADO", List.of())));
        when(mesas.mesa(5L)).thenReturn(Optional.of(new MesaResumen(5L, 12, EstadoMesa.LIBRE, true)));

        ComandaResponse respuesta = unicaComandaDe("PED-1");

        assertEquals(ComandaResponse.CANAL_SALON, respuesta.canal());
        assertEquals(12, respuesta.mesaNumero());
        assertNull(respuesta.entrega());
    }

    @Test
    void elPedidoDeLaAppParaRetirarIndicaRetiro() {
        when(pedidos.pedidoConfirmado("APP-1")).thenReturn(Optional.empty());
        when(clientes.metodoEntregaDe("APP-1")).thenReturn(Optional.of("RETIRAR"));

        ComandaResponse respuesta = unicaComandaDe("APP-1");

        assertEquals(ComandaResponse.CANAL_APP, respuesta.canal());
        assertNull(respuesta.mesaNumero());
        assertEquals("RETIRAR", respuesta.entrega());
    }

    @Test
    void elPedidoDeLaAppADomicilioIndicaDomicilio() {
        when(pedidos.pedidoConfirmado("APP-2")).thenReturn(Optional.empty());
        when(clientes.metodoEntregaDe("APP-2")).thenReturn(Optional.of("DOMICILIO"));

        assertEquals("DOMICILIO", unicaComandaDe("APP-2").entrega());
    }

    @Test
    void sinFormaDeEntregaConocidaSigueSiendoUnPedidoDeLaApp() {
        when(pedidos.pedidoConfirmado("APP-3")).thenReturn(Optional.empty());
        when(clientes.metodoEntregaDe("APP-3")).thenReturn(Optional.empty());

        ComandaResponse respuesta = unicaComandaDe("APP-3");

        assertEquals(ComandaResponse.CANAL_APP, respuesta.canal());
        assertNull(respuesta.entrega());
    }

    @Test
    void variasComandasDelMismoPedidoConsultanElOrigenUnaSolaVez() {
        when(pedidos.pedidoConfirmado("APP-4")).thenReturn(Optional.empty());
        when(clientes.metodoEntregaDe("APP-4")).thenReturn(Optional.of("RETIRAR"));
        when(comandas.buscarConLineas(null, null)).thenReturn(List.of(
                new Comanda("APP-4", 1, 3L, "Cocina"),
                new Comanda("APP-4", 2, 4L, "Barra")));

        List<ComandaResponse> respuestas = servicio.listar(null, null);

        assertEquals(2, respuestas.size());
        verify(pedidos, times(1)).pedidoConfirmado("APP-4");
        verify(clientes, times(1)).metodoEntregaDe("APP-4");
    }
}
