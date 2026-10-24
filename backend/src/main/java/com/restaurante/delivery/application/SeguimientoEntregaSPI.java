package com.restaurante.delivery.application;

import java.time.Instant;

/**
 * Seguimiento de la ENTREGA de un pedido a domicilio para la barra de progreso
 * del cliente (fase 4, app móvil). Es una vista de solo lectura que expone los
 * conceptos SEPARADOS sin filtrar PII ni identidad interna (no lleva clienteId
 * ni direccionId):
 *
 * <ul>
 *   <li>{@code faseLogistica}: avance operativo del reparto
 *       (SIN_ASIGNAR / ASIGNADO / EN_CAMINO / ENTREGADO_POR_REPARTIDOR / FALLIDA).</li>
 *   <li>{@code faseConfirmacion}: estado de la doble confirmación
 *       (SIN_CONFIRMAR / PENDIENTE_CONFIRMACION_CLIENTE / VERIFICADA / FALLIDA).</li>
 * </ul>
 *
 * <p>La barra de progreso combina estas fases con el estado de PREPARACIÓN del
 * pedido (CONFIRMADO/EN_PREPARACION/LISTO), que la app ya obtiene del snapshot
 * del pedido: esta vista solo aporta lo que el módulo delivery conoce.
 */
public record SeguimientoEntregaSPI(
        boolean tieneEntrega,
        String faseLogistica,
        String faseConfirmacion,
        Instant repartidorConfirmoAt,
        Instant clienteConfirmoAt,
        boolean entregaVerificada) {

    /** Sin entrega asociada (pedido no a domicilio o aún sin asignación). */
    public static SeguimientoEntregaSPI sinEntrega() {
        return new SeguimientoEntregaSPI(false, "SIN_ASIGNAR", "SIN_CONFIRMAR", null, null, false);
    }
}
