package com.restaurante.caja.infrastructure;

import com.restaurante.caja.domain.Caja;
import com.restaurante.caja.domain.EstadoCaja;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CajaRepository extends JpaRepository<Caja, Long> {

    Optional<Caja> findByEstado(EstadoCaja estado);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Caja c WHERE c.id = :id")
    Optional<Caja> findByIdParaActualizar(@Param("id") Long id);

    List<Caja> findAllByOrderByAbiertaAtDesc();
}