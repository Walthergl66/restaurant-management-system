package com.restaurante.delivery.infrastructure;

import com.restaurante.delivery.domain.Asignacion;
import com.restaurante.delivery.domain.EstadoAsignacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Acceso a las asignaciones de pedidos a repartidores (estado logístico).
 * Una asignación es ACTIVA mientras su estado no es REASIGNADA ni FALLIDA
 * (mismo criterio que el índice único parcial de la tabla, que impide dos
 * asignaciones activas para el mismo pedido).
 */
public interface AsignacionRepository extends JpaRepository<Asignacion, Long> {

    /** Asignación vigente (activa) de un pedido, si la hay. El índice único
     *  parcial garantiza que a lo sumo exista una. */
    @Query("""
            SELECT a FROM Asignacion a
            WHERE a.pedidoClienteId = :pedidoClienteId
              AND a.estado NOT IN (com.restaurante.delivery.domain.EstadoAsignacion.REASIGNADA,
                                   com.restaurante.delivery.domain.EstadoAsignacion.FALLIDA)
            """)
    Optional<Asignacion> findActivaByPedidoClienteId(@Param("pedidoClienteId") Long pedidoClienteId);

    List<Asignacion> findByRepartidorIdOrderByIdDesc(Long repartidorId);

    List<Asignacion> findByRecorridoIdOrderByIdAsc(Long recorridoId);

    /** Asignaciones activas (no terminales liberadoras) de un repartidor: lo
     *  que puede operar hoy (ASIGNADA/EN_RUTA/ENTREGADA). */
    @Query("""
            SELECT a FROM Asignacion a
            WHERE a.repartidorId = :repartidorId
              AND a.estado NOT IN (com.restaurante.delivery.domain.EstadoAsignacion.REASIGNADA,
                                   com.restaurante.delivery.domain.EstadoAsignacion.FALLIDA)
            ORDER BY a.id DESC
            """)
    List<Asignacion> findActivasByRepartidorId(@Param("repartidorId") Long repartidorId);

    List<Asignacion> findByEstadoOrderByIdDesc(EstadoAsignacion estado);

    List<Asignacion> findByEstadoAndRepartidorIdOrderByIdDesc(EstadoAsignacion estado, Long repartidorId);

    List<Asignacion> findAllByOrderByIdDesc();
}
