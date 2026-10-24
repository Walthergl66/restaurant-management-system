package com.restaurante.delivery.infrastructure;

import com.restaurante.delivery.domain.EstadoParada;
import com.restaurante.delivery.domain.Parada;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Acceso a las paradas de un recorrido. El orden lo fija ORS Optimization y se
 * persiste 0-based; las consultas lo respetan para devolver el recorrido en el
 * orden de visita.
 */
public interface ParadaRepository extends JpaRepository<Parada, Long> {

    List<Parada> findByRecorridoIdOrderByOrdenAsc(Long recorridoId);

    boolean existsByRecorridoIdAndEstado(Long recorridoId, EstadoParada estado);

    /**
     * Parada ACTIVA (más reciente) de un pedido, resuelta vía su asignación
     * activa. El índice único parcial de asignaciones garantiza a lo sumo una
     * asignación activa por pedido; se toma la parada de mayor id para quedarse
     * con la vigente. Sirve para localizar la parada que el cliente debe
     * confirmar a partir de su pedido.
     */
    @Query("""
            SELECT p FROM Parada p
            WHERE p.asignacionId IN (
                SELECT a.id FROM Asignacion a
                WHERE a.pedidoClienteId = :pedidoClienteId
                  AND a.estado NOT IN (com.restaurante.delivery.domain.EstadoAsignacion.REASIGNADA,
                                       com.restaurante.delivery.domain.EstadoAsignacion.FALLIDA)
            )
            ORDER BY p.id DESC
            """)
    List<Parada> buscarActivasPorPedidoClienteId(@Param("pedidoClienteId") Long pedidoClienteId);

    default Optional<Parada> findActivaByPedidoClienteId(Long pedidoClienteId) {
        return buscarActivasPorPedidoClienteId(pedidoClienteId).stream().findFirst();
    }

    /**
     * Paradas con el repartidor confirmado pero pendientes de la confirmación
     * del cliente (entrega aún NO verificada). Lo consulta el admin para
     * resolver incidencias.
     */
    @Query("""
            SELECT p FROM Parada p
            WHERE p.repartidorConfirmoAt IS NOT NULL
              AND p.entregaVerificada = false
              AND p.estado = com.restaurante.delivery.domain.EstadoParada.ENTREGADA
            ORDER BY p.repartidorConfirmoAt ASC
            """)
    List<Parada> buscarPendientesConfirmacionCliente();
}
