package com.restaurante.mesas;

import java.util.Optional;

/**
 * API pública del módulo de mesas. Vive en el paquete raíz para que otros
 * módulos (pedidos, cuentas...) consulten mesas sin romper el
 * encapsulamiento de Spring Modulith.
 */
public interface Mesas {

    Optional<MesaResumen> mesa(Long mesaId);

    /** Comprueba que la mesa existe y está habilitada para recibir pedidos nuevos. */
    void validarHabilitadaParaNuevoPedido(Long mesaId);
}
