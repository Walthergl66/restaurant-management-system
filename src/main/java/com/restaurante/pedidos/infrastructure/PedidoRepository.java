package com.restaurante.pedidos.infrastructure;

import com.restaurante.pedidos.domain.EstadoPedido;
import com.restaurante.pedidos.domain.Pedido;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
}