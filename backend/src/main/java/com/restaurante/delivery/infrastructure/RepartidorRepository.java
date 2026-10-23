package com.restaurante.delivery.infrastructure;

import com.restaurante.delivery.domain.EstadoRepartidor;
import com.restaurante.delivery.domain.Repartidor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Acceso a los repartidores (estado operativo). El vínculo con el usuario de
 * seguridad es por {@code usuarioId} (1:1, único en la tabla).
 */
public interface RepartidorRepository extends JpaRepository<Repartidor, Long> {

    Optional<Repartidor> findByUsuarioId(Long usuarioId);

    List<Repartidor> findAllByOrderByIdAsc();

    List<Repartidor> findByEstadoOrderByIdAsc(EstadoRepartidor estado);
}
