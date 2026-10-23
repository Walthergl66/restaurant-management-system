package com.restaurante.delivery.application;

import com.restaurante.delivery.domain.Coordenada;
import com.restaurante.delivery.domain.Parada;

import java.math.BigDecimal;

/**
 * Vista de una parada dentro de un recorrido para el repartidor: su orden de
 * visita (0-based), el estado logístico de la parada y la asignación (pedido)
 * que resuelve.
 *
 * <p>{@code lat}/{@code lng} son las coordenadas geocodificadas del destino de
 * la parada (desde la caché de geocoding). Pueden ser {@code null} si la
 * dirección no pudo geocodificarse; en ese caso el móvil no la posiciona en el
 * mapa pero el resto del flujo sigue funcionando.
 */
public record ParadaResumen(
        Long paradaId,
        Long asignacionId,
        Long pedidoClienteId,
        int orden,
        String estado,
        String codigo,
        String direccion,
        BigDecimal lat,
        BigDecimal lng,
        String faseConfirmacion,
        java.time.Instant repartidorConfirmoAt,
        java.time.Instant clienteConfirmoAt,
        boolean entregaVerificada) {

    static ParadaResumen de(Parada parada, EntregaResumen entrega, Coordenada coord) {
        return new ParadaResumen(
                parada.getId(),
                parada.getAsignacionId(),
                entrega != null ? entrega.pedidoClienteId() : null,
                parada.getOrden(),
                parada.getEstado().name(),
                entrega != null ? entrega.codigo() : null,
                entrega != null ? entrega.direccion() : null,
                coord != null ? coord.lat() : null,
                coord != null ? coord.lng() : null,
                parada.faseConfirmacion().name(),
                parada.getRepartidorConfirmoAt(),
                parada.getClienteConfirmoAt(),
                parada.isEntregaVerificada());
    }
}
