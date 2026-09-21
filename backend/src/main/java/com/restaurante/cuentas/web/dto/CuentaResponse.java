package com.restaurante.cuentas.web.dto;

import com.restaurante.anulaciones.AnulacionResumen;
import com.restaurante.cuentas.domain.Cuenta;
import com.restaurante.cuentas.domain.EstadoCuenta;
import com.restaurante.pedidos.LineaResumen;
import com.restaurante.pedidos.PedidoResumen;
import com.restaurante.shared.domain.Money;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Detalle de una cuenta: consumo por pedido, anulaciones aprobadas y el total
 * calculado siempre desde los registros (RNF-16).
 */
public record CuentaResponse(
        Long id,
        Long mesaId,
        int numeroMesa,
        EstadoCuenta estado,
        Long version,
        BigDecimal total,
        List<PedidoEnCuenta> pedidos,
        List<AnulacionEnCuenta> anulaciones,
        Instant createdAt,
        Instant updatedAt) {

    public record PedidoEnCuenta(
            String codigo,
            String estado,
            BigDecimal subtotal,
            List<LineaEnCuenta> lineas) {

        static PedidoEnCuenta from(PedidoResumen pedido) {
            List<LineaEnCuenta> lineas = pedido.lineas().stream().map(LineaEnCuenta::from).toList();
            return new PedidoEnCuenta(
                    pedido.codigo(),
                    pedido.estado(),
                    lineas.stream().map(LineaEnCuenta::subtotal)
                            .reduce(BigDecimal.ZERO, BigDecimal::add),
                    lineas);
        }
    }

    public record LineaEnCuenta(
            Long lineaId,
            Long productoId,
            String nombreProducto,
            int cantidad,
            BigDecimal precioUnitario,
            BigDecimal subtotal) {

        static LineaEnCuenta from(LineaResumen linea) {
            return new LineaEnCuenta(
                    linea.lineaId(),
                    linea.productoId(),
                    linea.nombreProducto(),
                    linea.cantidad(),
                    linea.precioUnitario(),
                    linea.subtotal());
        }
    }

    public record AnulacionEnCuenta(
            Long id,
            String pedidoCodigo,
            Long lineaId,
            String nombreProducto,
            BigDecimal precioUnitario,
            int cantidad,
            String motivo) {

        static AnulacionEnCuenta from(AnulacionResumen anulacion) {
            return new AnulacionEnCuenta(
                    anulacion.id(),
                    anulacion.pedidoCodigo(),
                    anulacion.lineaId(),
                    anulacion.nombreProducto(),
                    anulacion.precioUnitario(),
                    anulacion.cantidad(),
                    anulacion.motivo());
        }
    }

    public static CuentaResponse from(Cuenta cuenta, int numeroMesa,
                                      List<PedidoResumen> pedidos,
                                      List<AnulacionResumen> anulaciones,
                                      Money total) {
        return new CuentaResponse(
                cuenta.getId(),
                cuenta.getMesaId(),
                numeroMesa,
                cuenta.getEstado(),
                cuenta.getVersion(),
                total.getAmount(),
                pedidos.stream().map(PedidoEnCuenta::from).toList(),
                anulaciones.stream().map(AnulacionEnCuenta::from).toList(),
                cuenta.getCreatedAt(),
                cuenta.getUpdatedAt());
    }
}