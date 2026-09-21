package com.restaurante.pedidos.web.dto;

import com.restaurante.pedidos.domain.EstadoPedido;
import com.restaurante.pedidos.domain.Pedido;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Resumen completo de un pedido (RF-07 y RF-08: detalle y total por línea).
 */
public record PedidoResponse(
        String codigo,
        Long mesaId,
        EstadoPedido estado,
        String notas,
        BigDecimal total,
        List<LineaResponse> lineas,
        Instant createdAt,
        Instant updatedAt) {

    public static PedidoResponse from(Pedido pedido) {
        List<LineaResponse> lineas = pedido.getLineas().stream().map(LineaResponse::from).toList();
        return new PedidoResponse(
                pedido.getCodigo(),
                pedido.getMesaId(),
                pedido.getEstado(),
                pedido.getNotas().orElse(null),
                pedido.totalLineas().getAmount(),
                lineas,
                pedido.getCreatedAt(),
                pedido.getUpdatedAt());
    }
}