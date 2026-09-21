package com.restaurante.mesas.infrastructure;

import com.restaurante.mesas.domain.Mesa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MesaRepository extends JpaRepository<Mesa, Long> {

    boolean existsByNumero(int numero);

    List<Mesa> findByActivoTrueOrderByNumeroAsc();

    List<Mesa> findByActivoTrueAndEstadoOrderByNumeroAsc(
            com.restaurante.mesas.domain.EstadoMesa estado);
}