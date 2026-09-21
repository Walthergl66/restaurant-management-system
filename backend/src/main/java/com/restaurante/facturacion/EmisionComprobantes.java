package com.restaurante.facturacion;

import java.math.BigDecimal;

/**
 * Fábrica de comprobantes del sistema. Vive en el paquete raíz del módulo para
 * que otros módulos (pagos) emitan comprobantes respetando las fronteras de
 * Spring Modulith.
 */
public interface EmisionComprobantes {

    /**
     * Emite un comprobante con numeración secuencial (emisor interno; el SRI
     * será otra implementación de esta misma interfaz, RF-30).
     */
    ComprobanteResumen emitir(EmitirComprobante command);
}