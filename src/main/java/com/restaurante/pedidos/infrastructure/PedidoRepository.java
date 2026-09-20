package com.restaurante.pedidos.infrastructure;

import com.restaurante.pedidos.domain.EstadoPedido;
import com.restaurante.pedidos.domain.Pedido;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    Optional<Pedido> findByCodigo(String codigo);

    @EntityGraph(attributePaths = {"lineas", "lineas.extras", "lineas.ingredientesRemovidos", "confirmaciones"})
    Optional<Pedido> findByCodigoConLineas(String codigo);

    boolean existsByCodigo(String codigo);

    List<Pedido> findByMesaIdAndEstado(Long mesaId, EstadoPedido estado);
}