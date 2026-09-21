package com.restaurante.pagos.infrastructure;

import com.restaurante.pagos.domain.Pago;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PagoRepository extends JpaRepository<Pago, Long> {

    List<Pago> findByCuentaId(Long cuentaId);

    List<Pago> findByCajaId(Long cajaId);
}