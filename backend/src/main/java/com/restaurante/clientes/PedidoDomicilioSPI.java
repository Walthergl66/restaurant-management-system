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
 */
public record PedidoDomicilioSPI(
        Long pedidoClienteId,
        String codigo,
        String estado,
        String metodoEntrega,
        String clienteNombre,
        String direccion,
        String telefono) {
}
