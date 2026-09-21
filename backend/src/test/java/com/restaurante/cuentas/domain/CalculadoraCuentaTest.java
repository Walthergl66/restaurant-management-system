package com.restaurante.cuentas.domain;

import com.restaurante.anulaciones.AnulacionResumen;
import com.restaurante.pedidos.LineaResumen;
import com.restaurante.pedidos.PedidoResumen;
import com.restaurante.shared.domain.Money;
import com.restaurante.shared.domain.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * RNF-16: el total de la cuenta SOLO se calcula desde los registros (pedidos
 * confirmados menos anulaciones aprobadas), en esta clase, sin importar el
 * orden en que lleguen los eventos.
 */
class CalculadoraCuentaTest {

    private final CalculadoraCuenta calculadora = new CalculadoraCuenta();

    @Test
    void totalEsSumaDePedidosSinAnulaciones() {
        List<PedidoResumen> pedidos = List.of(
                pedido("P-1", linea(1L, "Ceviche", 1, "10.00")),
                pedido("P-2", linea(2L, "Arroz", 3, "4.00")));

        Money total = calculadora.total(pedidos, List.of());

        assertEquals("22.00", total.getAmount().toPlainString());
    }

    @Test
    void anulacionesAprobadasDescuentan() {
        List<PedidoResumen> pedidos = List.of(
                pedido("P-1", linea(1L, "Ceviche", 2, "10.00")));
        List<AnulacionResumen> aprobadas = List.of(
                anulacion("P-1", 1L, "Ceviche", "10.00", 1));

        Money total = calculadora.total(pedidos, aprobadas);

        assertEquals("10.00", total.getAmount().toPlainString());
    }

    @Test
    void esIndependienteDelOrdenDePedidosYAnulaciones() {
        List<PedidoResumen> conDosPedidos = List.of(
                pedido("P-1", linea(1L, "A", 2, "10.00")),
                pedido("P-2", linea(2L, "B", 1, "5.00")));
        List<AnulacionResumen> aprobadas = List.of(
                anulacion("P-1", 1L, "A", "10.00", 1),
                anulacion("P-2", 2L, "B", "5.00", 1));

        Money primero = calculadora.total(conDosPedidos, aprobadas);
        Money invertido = calculadora.total(
                List.of(conDosPedidos.get(1), conDosPedidos.get(0)),
                List.of(aprobadas.get(1), aprobadas.get(0)));

        assertEquals("15.00", primero.getAmount().toPlainString());
        assertEquals(primero, invertido);
    }

    @Test
    void totalNegativoLanzaReglaDeNegocio() {
        List<PedidoResumen> pedidos = List.of(
                pedido("P-1", linea(1L, "A", 1, "2.00")));
        List<AnulacionResumen> aprobadas = List.of(
                anulacion("P-1", 1L, "A", "2.00", 2));

        assertThrows(BusinessRuleException.class, () -> calculadora.total(pedidos, aprobadas));
    }

    @Test
    void sinRegistrosElTotalEsCero() {
        Money total = calculadora.total(List.of(), List.of());
        assertEquals("0.00", total.getAmount().toPlainString());
    }

    private PedidoResumen pedido(String codigo, LineaResumen linea) {
        return new PedidoResumen(codigo, 7L, "CONFIRMADO", List.of(linea));
    }

    private LineaResumen linea(Long id, String nombre, int cantidad, String precio) {
        BigDecimal unitario = new BigDecimal(precio);
        return new LineaResumen(id, id + 1000, nombre, cantidad, unitario,
                unitario.multiply(new BigDecimal(cantidad)), 1L, "Cocina");
    }

    private AnulacionResumen anulacion(String pedido, Long lineaId, String nombre, String precio, int cantidad) {
        return new AnulacionResumen(
                pedido.hashCode() + lineaId, pedido, lineaId, lineaId + 1000, nombre,
                new BigDecimal(precio), cantidad, "cliente pidió quitarlo", 1L, "Cocina", "APROBADA");
    }
}