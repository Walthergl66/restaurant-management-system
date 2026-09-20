package com.restaurante.configuracion.web.dto;

import com.restaurante.configuracion.domain.Parametro;
import com.restaurante.configuracion.domain.TipoParametro;

/**
 * Vista de un parámetro de configuración.
 */
public record ParametroResponse(
        String clave,
        String valor,
        TipoParametro tipo,
        String descripcion) {

    public static ParametroResponse from(Parametro parametro) {
        return new ParametroResponse(
                parametro.getClave(),
                parametro.getValor(),
                parametro.getTipo(),
                parametro.getDescripcion());
    }
}