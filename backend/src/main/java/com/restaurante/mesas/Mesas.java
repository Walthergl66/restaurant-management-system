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

    /** Cuentas: la mesa pasa a OCUPADA cuando se abre su cuenta. Idempotente; falla si la mesa no existe. */
    void marcarOcupada(Long mesaId);

    /** Cuentas: la mesa pasa a LIBRE cuando se cierra su cuenta. Idempotente; falla si la mesa no existe. */
    void liberar(Long mesaId);
}
