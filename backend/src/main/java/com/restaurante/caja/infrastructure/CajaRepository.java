package com.restaurante.caja.infrastructure;

import com.restaurante.caja.domain.Caja;
import com.restaurante.caja.domain.EstadoCaja;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CajaRepository extends JpaRepository<Caja, Long> {

    Optional<Caja> findByEstado(EstadoCaja estado);

    List<Caja> findAllByOrderByAbiertaAtDesc();
}