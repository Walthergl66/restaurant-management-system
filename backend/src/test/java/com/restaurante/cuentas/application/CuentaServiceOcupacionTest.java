package com.restaurante.cuentas.application;

import com.restaurante.anulaciones.Anulaciones;
import com.restaurante.cuentas.domain.Cuenta;
import com.restaurante.cuentas.domain.EstadoCuenta;
import com.restaurante.cuentas.infrastructure.CuentaRepository;
import com.restaurante.mesas.Mesas;
import com.restaurante.pedidos.PedidoCancelado;
import com.restaurante.pedidos.PedidoCreado;
import com.restaurante.pedidos.PedidoResumen;
import com.restaurante.pedidos.Pedidos;
import com.restaurante.shared.domain.exception.BusinessRuleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** La mesa queda ocupada mientras tenga una cuenta abierta y se libera al cerrarse por cualquier camino. */
class CuentaServiceOcupacionTest {

    private static final long MESA = 7L;

    private CuentaRepository cuentas;
    private Pedidos pedidos;
    private Mesas mesas;
    private CuentaService servicio;

    private Cuenta cuenta(long id) {
        Cuenta cuenta = new Cuenta(MESA);
        ReflectionTestUtils.setField(cuenta, "id", id);
        return cuenta;
    }

    @BeforeEach
    void preparar() {
        cuentas = mock(CuentaRepository.class);
        pedidos = mock(Pedidos.class);
        mesas = mock(Mesas.class);
        servicio = new CuentaService(cuentas, pedidos, mock(Anulaciones.class), mesas);
        when(cuentas.save(any(Cuenta.class))).thenAnswer(i -> {
            Cuenta nueva = i.getArgument(0);
            ReflectionTestUtils.setField(nueva, "id", 100L);
            return nueva;
        });
    }

    @Test
    void abrirUnaCuentaNuevaOcupaLaMesa() {
        when(cuentas.findByMesaIdAndEstado(MESA, EstadoCuenta.ABIERTA)).thenReturn(Optional.empty());

        servicio.alCrearPedido(new PedidoCreado("PED-1", MESA));

        verify(mesas).marcarOcupada(MESA);
        verify(pedidos).asignarCuenta("PED-1", 100L);
    }

    @Test
    void reutilizarUnaCuentaAbiertaNoVuelveAOcuparLaMesa() {
        when(cuentas.findByMesaIdAndEstado(MESA, EstadoCuenta.ABIERTA)).thenReturn(Optional.of(cuenta(5L)));

        servicio.alCrearPedido(new PedidoCreado("PED-2", MESA));

        verify(mesas, never()).marcarOcupada(any());
        verify(pedidos).asignarCuenta("PED-2", 5L);
    }

    @Test
    void cerrarLaCuentaAManoLiberaLaMesa() {
        when(cuentas.findById(5L)).thenReturn(Optional.of(cuenta(5L)));
        when(pedidos.pedidosDeCuenta(5L)).thenReturn(List.of());

        servicio.cerrar(5L);

        verify(mesas).liberar(MESA);
    }

    @Test
    void cerrarParaCobroLiberaLaMesa() {
        when(cuentas.findById(5L)).thenReturn(Optional.of(cuenta(5L)));
        when(pedidos.pedidosDeCuenta(5L)).thenReturn(List.of());

        servicio.cerrarParaCobro(5L);

        verify(mesas).liberar(MESA);
    }

    @Test
    void siElCierreSeRechazaPorBorradoresLaMesaSigueOcupada() {
        when(cuentas.findById(5L)).thenReturn(Optional.of(cuenta(5L)));
        when(pedidos.pedidosDeCuenta(5L)).thenReturn(List.of(new PedidoResumen("PED-3", MESA, "BORRADOR", List.of())));

        assertThrows(BusinessRuleException.class, () -> servicio.cerrar(5L));

        verify(mesas, never()).liberar(any());
    }

    @Test
    void cancelarElUltimoBorradorCierraLaCuentaVaciaYLiberaLaMesa() {
        Cuenta abierta = cuenta(5L);
        when(pedidos.pedidosDeMesa(MESA)).thenReturn(List.of());
        when(cuentas.findByMesaIdAndEstado(MESA, EstadoCuenta.ABIERTA)).thenReturn(Optional.of(abierta));

        servicio.alCancelarPedido(new PedidoCancelado("PED-4", MESA));

        assertEquals(EstadoCuenta.CERRADA, abierta.getEstado());
        verify(mesas).liberar(MESA);
    }

    @Test
    void cancelarUnBorradorConOtrosPedidosEnLaMesaNoLiberaLaMesa() {
        when(pedidos.pedidosDeMesa(MESA)).thenReturn(List.of(new PedidoResumen("PED-5", MESA, "CONFIRMADO", List.of())));

        servicio.alCancelarPedido(new PedidoCancelado("PED-6", MESA));

        verify(mesas, never()).liberar(any());
    }
}
