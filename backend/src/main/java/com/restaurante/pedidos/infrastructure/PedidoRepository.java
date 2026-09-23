package com.restaurante.pedidos.infrastructure;

import com.restaurante.pedidos.domain.EstadoPedido;
import com.restaurante.pedidos.domain.Pedido;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    Optional<Pedido> findByCodigo(String codigo);

    /**
     * Pedido completo con sus líneas (extras e ingredientes) y confirmaciones,
     * listo para resumir o confirmar.
     */
    @EntityGraph(attributePaths = {"lineas", "lineas.extras", "lineas.ingredientesRemovidos", "confirmaciones"})
    @Query("SELECT p FROM Pedido p WHERE p.codigo = :codigo")
    Optional<Pedido> findByCodigoConRelaciones(@Param("codigo") String codigo);

    boolean existsByCodigo(String codigo);

    List<Pedido> findByMesaIdAndEstado(Long mesaId, EstadoPedido estado);

    @EntityGraph(attributePaths = {"lineas", "lineas.extras", "lineas.ingredientesRemovidos"})
    List<Pedido> findByEstadoNot(EstadoPedido estado);

    /**
     * Pedidos no anulados de una mesa, con sus líneas, para totalizar la cuenta.
     */
    @EntityGraph(attributePaths = {"lineas", "lineas.extras", "lineas.ingredientesRemovidos"})
    List<Pedido> findByMesaIdAndEstadoNot(Long mesaId, EstadoPedido estado);

    /**
     * Pedidos vendibles del período (sin borradores ni anulados), con sus
     * líneas, para el reporte de ventas por producto (RF-51). El filtro va en
     * la base de datos, no en memoria.
     */
    @EntityGraph(attributePaths = {"lineas", "lineas.extras", "lineas.ingredientesRemovidos"})
    List<Pedido> findByCreatedAtBetweenAndEstadoNotIn(Instant desde, Instant hasta,
                                                      Collection<EstadoPedido> excluidos);

    /**
     * True si la mesa tiene otro pedido no anulado distinto del indicado.
     */
    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN TRUE ELSE FALSE END FROM Pedido p "
            + "WHERE p.mesaId = :mesaId AND p.codigo <> :codigo AND p.estado <> com.restaurante.pedidos.domain.EstadoPedido.ANULADO")
    boolean existeOtroPedidoEnMesa(@Param("mesaId") Long mesaId, @Param("codigo") String codigo);
}