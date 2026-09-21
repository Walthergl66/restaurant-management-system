package com.restaurante.mesas;

import java.util.Optional;

/**
 * API pública del módulo de mesas. Vive en el paquete raíz para que otros
 * módulos (pedidos, cuentas...) ocupen y liberen mesas sin romper el
 * encapsulamiento de Spring Modulith.
 */
public interface Mesas {

    Optional<MesaResumen> mesa(Long mesaId);

    /**
     * Marca la mesa como ocupada (solo desde LIBRE o RESERVADA).
     */
    void ocuparMesa(Long mesaId);

    /**
     * Libera la mesa.
     */
    void liberarMesa(Long mesaId);
}