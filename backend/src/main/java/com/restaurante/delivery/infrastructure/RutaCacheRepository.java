package com.restaurante.delivery.infrastructure;

import com.restaurante.delivery.domain.RutaCache;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Acceso a la caché de rutas. {@code clave_hash} identifica el conjunto
 * origen+paradas ordenadas+criterio; se reutiliza mientras no haya expirado
 * para no repetir el cálculo en ORS Directions.
 */
public interface RutaCacheRepository extends JpaRepository<RutaCache, Long> {

    Optional<RutaCache> findByClaveHash(String claveHash);

    void deleteByClaveHash(String claveHash);
}
