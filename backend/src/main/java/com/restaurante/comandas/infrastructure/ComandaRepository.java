package com.restaurante.comandas.infrastructure;

import com.restaurante.comandas.domain.Comanda;
import com.restaurante.comandas.domain.ComandaEstado;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ComandaRepository extends JpaRepository<Comanda, Long> {

    @Query(value = "SELECT nextval('seq_numero_comanda')", nativeQuery = true)
    Long siguienteNumeroComanda();

    @EntityGraph(attributePaths = "lineas")
    @Query("select c from Comanda c")
    List<Comanda> findAllConLineas();

    @EntityGraph(attributePaths = "lineas")
    @Query("select c from Comanda c where c.areaId = :areaId")
    List<Comanda> findPorAreaConLineas(Long areaId);

    @EntityGraph(attributePaths = "lineas")
    @Query("select c from Comanda c where c.estado = :estado")
    List<Comanda> findPorEstadoConLineas(ComandaEstado estado);

    @EntityGraph(attributePaths = "lineas")
    @Query("select c from Comanda c"
            + " where (:areaId is null or c.areaId = :areaId)"
            + " and (:estado is null or c.estado = :estado)")
    List<Comanda> buscarConLineas(Long areaId, ComandaEstado estado);

    @EntityGraph(attributePaths = "lineas")
    @Query("select c from Comanda c where c.id = :id")
    Optional<Comanda> findByIdConLineas(Long id);

    boolean existsByPedidoCodigoAndAreaId(String pedidoCodigo, Long areaId);

    /**
     * Idempotencia de la comanda de cancelación: una sola por anulación aprobada.
     */
    boolean existsByAnulacionId(Long anulacionId);
}