package com.restaurante.delivery.infrastructure;

import com.restaurante.delivery.domain.ConfigRestaurante;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Acceso a la configuración del restaurante. Es un registro único (fila única
 * garantizada por índice en la tabla), así que {@link #findFirstByOrderByIdAsc()}
 * devuelve siempre esa fila.
 */
public interface ConfigRestauranteRepository extends JpaRepository<ConfigRestaurante, Long> {

    Optional<ConfigRestaurante> findFirstByOrderByIdAsc();
}
