package com.restaurante.facturacion;

import com.restaurante.facturacion.domain.Comprobante;

/**
 * Estrategia de emisión con el correlativo del establecimiento (numeración
 * secuencial "R-000001"). El SRI será otra implementación de
 * {@link EmisionComprobantes}, no de esta estrategia.
 */
public interface EmisorInterno {

    Comprobante emitir(EmitirComprobante command, Long secuencial);
}