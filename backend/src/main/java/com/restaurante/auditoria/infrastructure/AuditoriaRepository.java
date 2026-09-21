package com.restaurante.auditoria.infrastructure;

import com.restaurante.auditoria.domain.EventoAuditoria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface AuditoriaRepository extends JpaRepository<EventoAuditoria, Long> {

    List<EventoAuditoria> findByFechaBetweenOrderByFechaDesc(Instant desde, Instant hasta);
}