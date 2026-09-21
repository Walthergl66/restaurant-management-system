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
        String estado,
        List<ComandaLineaResponse> lineas) {

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

    public static ComandaResponse from(Comanda comanda) {
        return new ComandaResponse(
                comanda.getId(),
                comanda.getPedidoCodigo(),
                comanda.getNumeroComanda(),
                comanda.getAreaId(),
                comanda.getAreaNombre(),
                comanda.getEstado().name(),
                comanda.getLineas().stream().map(ComandaLineaResponse::from).toList());
    }
}