package com.restaurante.delivery.infrastructure;

import com.restaurante.delivery.domain.Incidencia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Acceso a las incidencias de entregas fallidas (solo inserción y lectura; el
 * registro es inmutable y se conserva para trazabilidad).
 */
public interface IncidenciaRepository extends JpaRepository<Incidencia, Long> {

    List<Incidencia> findByParadaId(Long paradaId);
}
