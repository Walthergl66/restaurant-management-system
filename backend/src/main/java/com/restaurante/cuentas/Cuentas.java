package com.restaurante.cuentas;

/**
 * API pública del módulo de cuentas para otros módulos (pagos cobran y cierran
 * la cuenta, reportes leen el total). Vive en el paquete raíz para respetar
 * las fronteras de Spring Modulith.
 */
public interface Cuentas {

    /**
     * Resumen con el total SIEMPRE calculado desde los registros (RNF-16).
     */
    CuentaResumen resumen(Long cuentaId);

    /**
     * Cierra la cuenta exigiendo que no haya borradores y libera la mesa.
     */
    void cerrarParaCobro(Long cuentaId);
}