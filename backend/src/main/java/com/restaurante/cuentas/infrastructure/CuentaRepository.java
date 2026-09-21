package com.restaurante.cuentas.infrastructure;

import com.restaurante.cuentas.domain.Cuenta;
import com.restaurante.cuentas.domain.EstadoCuenta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CuentaRepository extends JpaRepository<Cuenta, Long> {

    Optional<Cuenta> findByMesaIdAndEstado(Long mesaId, EstadoCuenta estado);

    List<Cuenta> findByMesaId(Long mesaId);

    List<Cuenta> findByEstado(EstadoCuenta estado);
}