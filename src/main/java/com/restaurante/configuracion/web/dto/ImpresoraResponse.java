package com.restaurante.configuracion.web.dto;

import com.restaurante.configuracion.domain.Impresora;
import com.restaurante.configuracion.domain.TipoImpresora;

/**
 * Vista de una impresora.
 */
public record ImpresoraResponse(
        Long id,
        String nombre,
        TipoImpresora tipo,
        String ip,
        int puerto,
        String area,
        boolean activo) {

    public static ImpresoraResponse from(Impresora impresora) {
        return new ImpresoraResponse(
                impresora.getId(),
                impresora.getNombre(),
                impresora.getTipo(),
                impresora.getIp(),
                impresora.getPuerto(),
                impresora.getArea(),
                impresora.isActivo());
    }
}