package com.restaurante.delivery.infrastructure;

import com.restaurante.delivery.domain.EstadoRecorrido;
import com.restaurante.delivery.domain.Recorrido;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Acceso a los recorridos de un repartidor (estado logístico). Las consultas
 * se acotan SIEMPRE por {@code repartidorId} para que un repartidor solo vea
 * lo suyo; la verificación de propiedad real la refuerza la capa de aplicación.
 */
public interface RecorridoRepository extends JpaRepository<Recorrido, Long> {

    List<Recorrido> findByRepartidorIdOrderByIdDesc(Long repartidorId);

    List<Recorrido> findByRepartidorIdAndEstadoInOrderByIdDesc(
            Long repartidorId, List<EstadoRecorrido> estados);
}
