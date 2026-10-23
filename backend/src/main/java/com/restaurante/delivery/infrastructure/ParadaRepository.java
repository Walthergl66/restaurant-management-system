package com.restaurante.delivery.infrastructure;

import com.restaurante.delivery.domain.EstadoParada;
import com.restaurante.delivery.domain.Parada;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Acceso a las paradas de un recorrido. El orden lo fija ORS Optimization y se
 * persiste 0-based; las consultas lo respetan para devolver el recorrido en el
 * orden de visita.
 */
public interface ParadaRepository extends JpaRepository<Parada, Long> {

    List<Parada> findByRecorridoIdOrderByOrdenAsc(Long recorridoId);

    boolean existsByRecorridoIdAndEstado(Long recorridoId, EstadoParada estado);
}
