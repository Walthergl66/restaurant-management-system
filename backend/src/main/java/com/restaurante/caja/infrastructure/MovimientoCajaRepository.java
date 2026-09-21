package com.restaurante.caja.infrastructure;

import com.restaurante.caja.domain.MovimientoCaja;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface MovimientoCajaRepository extends JpaRepository<MovimientoCaja, Long> {

    List<MovimientoCaja> findByCajaId(Long cajaId);

    /**
     * Movimientos del período, para finanzas (RF-36 a RF-39). El filtro va en
     * la base de datos, no en memoria.
     */
    List<MovimientoCaja> findByCreatedAtBetween(Instant desde, Instant hasta);
}