package com.restaurante.clientes.infrastructure;

import com.restaurante.clientes.domain.EstadoPedidoCliente;
import com.restaurante.clientes.domain.PedidoCliente;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PedidoClienteRepository extends JpaRepository<PedidoCliente, Long> {

    Optional<PedidoCliente> findByCodigo(String codigo);

    @EntityGraph(attributePaths = {"lineas", "lineas.extras"})
    @Query("SELECT p FROM PedidoCliente p WHERE p.codigo = :codigo")
    Optional<PedidoCliente> findByCodigoWithLineas(@Param("codigo") String codigo);

    boolean existsByCodigo(String codigo);

    Optional<PedidoCliente> findByClienteIdAndIdempotencyKey(Long clienteId, String idempotencyKey);

    List<PedidoCliente> findByClienteId(Long clienteId);

    List<PedidoCliente> findByClienteIdAndEstado(Long clienteId, EstadoPedidoCliente estado);

    @EntityGraph(attributePaths = {"lineas", "lineas.extras"})
    @Query("SELECT p FROM PedidoCliente p WHERE p.clienteId = :clienteId")
    List<PedidoCliente> findByClienteIdWithLineas(@Param("clienteId") Long clienteId);
}
