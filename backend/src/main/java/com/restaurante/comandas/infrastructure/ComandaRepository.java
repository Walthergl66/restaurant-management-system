package com.restaurante.comandas.infrastructure;

import com.restaurante.comandas.domain.Comanda;
import com.restaurante.comandas.domain.ComandaEstado;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
     * Comandas de un pedido, bloqueadas para escritura y en orden por id. Es la
     * barrera que impide que dos áreas se marquen a la vez leyendo cada una la
     * otra sin commitear: con una comanda por área, decidir "todas preparadas"
     * sobre una foto inconsistente puede dejar el pedido sin avanzar nunca.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Comanda c where c.pedidoCodigo = :pedidoCodigo order by c.id")
    List<Comanda> findPorPedidoParaActualizar(@Param("pedidoCodigo") String pedidoCodigo);

    /**
     * Comandas de un pedido, por área. Es lo que permite decidir si el pedido
     * puede avanzar: con una comanda por área, un área sin terminar significa
     * pedido incompleto.
     */
    @Query("select c from Comanda c where c.pedidoCodigo = :pedidoCodigo")
    List<Comanda> findPorPedido(@Param("pedidoCodigo") String pedidoCodigo);

    /**
     * Idempotencia de la comanda de cancelación: una sola por anulación aprobada.
     */
    boolean existsByAnulacionId(Long anulacionId);
}