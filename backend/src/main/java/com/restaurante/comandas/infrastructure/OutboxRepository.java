package com.restaurante.comandas.infrastructure;

import com.restaurante.comandas.domain.EstadoOutbox;
import com.restaurante.comandas.domain.EventoOutbox;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface OutboxRepository extends JpaRepository<EventoOutbox, Long> {

    @Query("select o from EventoOutbox o where o.estado = :estado order by o.creadaAt asc")
    List<EventoOutbox> buscarPorEstado(EstadoOutbox estado, Pageable pageable);

    Optional<EventoOutbox> findByTipoAndAgregadoIdAndEvento(String tipo, String agregadoId, String evento);
}