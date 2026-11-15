package com.restaurante.clientes.web.dto;

import com.restaurante.clientes.domain.EstadoPedidoCliente;
import com.restaurante.shared.domain.Money;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Respuesta de consulta de un pedido del cliente (snapshot para la app web/móvil).
 * DTO de solo lectura: expone los campos del contrato JSON sin exponer la entidad JPA.
 */
public record PedidoClienteResponse(
        String codigo,
        EstadoPedidoCliente estado,
        String metodoPago,
        String metodoEntrega,
        Long direccionId,
        BigDecimal costoEntrega,
        BigDecimal total,
        Instant confirmadoAt,
        Instant creadoAt,
        Instant actualizadoAt,
        List<PedidoClienteLineaResponse> lineas) {

    public PedidoClienteResponse {
        costoEntrega = costoEntrega == null ? BigDecimal.ZERO : costoEntrega;
        total = total == null ? BigDecimal.ZERO : total;
        lineas = lineas == null ? List.of() : lineas;
    }

    public static PedidoClienteResponse from(com.restaurante.clientes.domain.PedidoCliente pedido) {
        List<PedidoClienteLineaResponse> lineas = pedido.getLineas().stream()
                .map(PedidoClienteLineaResponse::from)
                .toList();
        BigDecimal total = lineas.stream()
                .map(l -> l.subtotal())
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .add(pedido.getCostoEntrega().getAmount());
        return new PedidoClienteResponse(
                pedido.getCodigo(),
                pedido.getEstado(),
                pedido.getMetodoPago(),
                pedido.getMetodoEntrega(),
                pedido.getDireccionId(),
                pedido.getCostoEntrega().getAmount(),
                total,
                pedido.getConfirmadoAt(),
                pedido.getCreadoAt(),
                pedido.getActualizadoAt(),
                lineas);
    }
}
