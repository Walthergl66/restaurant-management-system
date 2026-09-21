package com.restaurante.caja.infrastructure;

import com.restaurante.caja.domain.MovimientoCaja;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MovimientoCajaRepository extends JpaRepository<MovimientoCaja, Long> {

    List<MovimientoCaja> findByCajaId(Long cajaId);
}