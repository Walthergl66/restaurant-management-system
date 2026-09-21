package com.restaurante.pagos.infrastructure;

import com.restaurante.pagos.domain.Pago;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface PagoRepository extends JpaRepository<Pago, Long> {

    List<Pago> findByCuentaId(Long cuentaId);

    List<Pago> findByCajaId(Long cajaId);

    /**
     * Pagos del período, para finanzas y reportes (RF-36 a RF-39, RF-51). El
     * filtro va en la base de datos, no en memoria.
     */
    List<Pago> findByCreatedAtBetween(Instant desde, Instant hasta);
}