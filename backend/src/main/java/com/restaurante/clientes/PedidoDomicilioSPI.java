package com.restaurante.clientes;

/**
 * Vista mínima que el módulo delivery consume para asignar un pedido a
 * domicilio a un repartidor. Expone los datos de entrega del pedido del
 * cliente (RF-42/RF-44) sin filtrar su identidad interna: el id es el de
 * {@code comercial.pedidos_clientes} (al que apunta {@code delivery.asignaciones.pedido_cliente_id}).
 *
 * <p>Contiene PII (nombre, dirección, teléfono del cliente): el módulo
 * delivery solo debe exponerla a usuarios con permiso de administración de
 * asignaciones.
 *
 * <p>{@code direccionId} es el id de {@code comercial.clientes_direcciones}; el
 * módulo delivery lo usa como clave estable para cachear el geocoding de la
 * dirección (la dirección es TEXTO, sin coordenadas) y evitar regeocodificar.
 * Puede ser {@code null} si el pedido no tiene dirección asociada.
 */
public record PedidoDomicilioSPI(
        Long pedidoClienteId,
        String codigo,
        String estado,
        String metodoEntrega,
        String clienteNombre,
        Long direccionId,
        String direccion,
        String telefono,
        java.math.BigDecimal lat,
        java.math.BigDecimal lng) {
}
