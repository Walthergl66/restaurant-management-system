package com.restaurante.caja;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * API pública del módulo de caja para otros módulos (pagos registran los
 * cobros como ingresos en la caja abierta). Vive en el paquete raíz para
 * respetar las fronteras de Spring Modulith.
 */
public interface Cajas {

    Optional<CajaResumen> cajaAbierta();

    /**
     * Registra el ingreso de un cobro en la caja (con la referencia al pago).
     */
    void registrarIngreso(Long cajaId, String concepto, BigDecimal monto,
                          String metodo, Long pagoId);
}