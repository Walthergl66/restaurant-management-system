package com.restaurante.delivery.infrastructure;

import com.restaurante.delivery.domain.GeocodingCache;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Acceso a la caché de geocoding. Única por {@code direccion_id}: se reutiliza
 * para no regeocodificar la misma dirección y no gastar la cuota de ORS.
 */
public interface GeocodingCacheRepository extends JpaRepository<GeocodingCache, Long> {

    Optional<GeocodingCache> findByDireccionId(Long direccionId);
}
