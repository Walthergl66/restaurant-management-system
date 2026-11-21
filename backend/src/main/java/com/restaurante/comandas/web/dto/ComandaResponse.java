package com.restaurante.comandas.web.dto;

import com.restaurante.comandas.domain.ComandaLinea;
import com.restaurante.comandas.domain.Comanda;

import java.util.List;

public record ComandaResponse(
        Long id,
        String pedidoCodigo,
        int numeroComanda,
        Long areaId,
        String areaNombre,
        String tipo,
        String estado,
        Integer mesaNumero,
        String canal,
        List<ComandaLineaResponse> lineas) {

    /** Pedido tomado en el salón (tiene mesa). */
    public static final String CANAL_SALON = "SALON";
    /** Pedido hecho por el cliente desde la app (retiro o domicilio): no tiene mesa. */
    public static final String CANAL_APP = "APP";

    public record ComandaLineaResponse(
            Long productoId,
            String nombreProducto,
            int cantidad,
            String extras,
            String ingredientes,
            String observaciones,
            int orden) {

        static ComandaLineaResponse from(ComandaLinea linea) {
            return new ComandaLineaResponse(
                    linea.getProductoId(),
                    linea.getNombreProducto(),
                    linea.getCantidad(),
                    linea.getExtras(),
                    linea.getIngredientes(),
                    linea.getObservaciones(),
                    linea.getOrden());
        }
    }

    /** @param mesaNumero número de la mesa del pedido de salón; {@code null} en pedidos de la app. */
    public static ComandaResponse from(Comanda comanda, Integer mesaNumero, String canal) {
        return new ComandaResponse(
                comanda.getId(),
                comanda.getPedidoCodigo(),
                comanda.getNumeroComanda(),
                comanda.getAreaId(),
                comanda.getAreaNombre(),
                comanda.getTipo().name(),
                comanda.getEstado().name(),
                mesaNumero,
                canal,
                comanda.getLineas().stream().map(ComandaLineaResponse::from).toList());
    }
}