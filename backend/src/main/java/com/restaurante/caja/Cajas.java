package com.restaurante.caja;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
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

    /**
     * Movimientos (ingresos y egresos) entre dos instantes, para finanzas.
     */
    List<MovimientoRegistro> movimientosEnPeriodo(Instant desde, Instant hasta);

    record MovimientoRegistro(String tipo, String metodo, BigDecimal monto,
                              Long pagoId, Instant fecha) {
    }
}