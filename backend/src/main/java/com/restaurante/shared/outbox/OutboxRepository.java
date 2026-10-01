package com.restaurante.shared.outbox;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio del outbox compartido (tabla V4). Cada módulo filtra por su
 * {@code tipo} para no ver las órdenes de los demás.
 */
public interface OutboxRepository extends JpaRepository<EventoOutbox, Long> {

    @Query("select o from EventoOutbox o where o.estado = :estado order by o.creadaAt asc")
    List<EventoOutbox> buscarPorEstado(EstadoOutbox estado, Pageable pageable);

    @Query("select o from EventoOutbox o"
            + " where o.tipo = :tipo and o.estado in :estados order by o.creadaAt asc")
    List<EventoOutbox> buscarPorTipoYEstados(String tipo, Collection<EstadoOutbox> estados, Pageable pageable);

    Optional<EventoOutbox> findByTipoAndAgregadoIdAndEvento(String tipo, String agregadoId, String evento);

    /**
     * Evento de un tipo concreto. El outbox es una tabla compartida, así que
     * cada módulo debe acotar por su {@code tipo}: sin el filtro, un
     * acknowledgment puede marcar como enviado un evento que no es suyo.
     */
    Optional<EventoOutbox> findByTipoAndId(String tipo, Long id);

    /** Órdenes en un estado con antigüedad mayor o igual al corte (monitoreo). */
    @Query("select count(o) from EventoOutbox o where o.estado = :estado and o.creadaAt <= :corte")
    long contarPorEstadoAntesDe(EstadoOutbox estado, Instant corte);
}