package com.restaurante.clientes;

import java.util.List;

/**
 * Vista de un pedido de la app para el mostrador (mesero o cocina): qué es, de quién y qué lleva.
 * No incluye datos de entrega ni de pago.
 *
 * @param entrega "RETIRAR" o "DOMICILIO"
 */
public record PedidoMostradorSPI(
        String codigo,
        String estado,
        String entrega,
        String clienteNombre,
        List<LineaMostrador> lineas) {

    public record LineaMostrador(String nombreProducto, int cantidad) {
    }
}
