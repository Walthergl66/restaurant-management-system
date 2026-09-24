package com.restaurante.clientes.infrastructure;

import com.restaurante.clientes.domain.EstadoPedidoCliente;
import com.restaurante.clientes.domain.PedidoCliente;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

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

    /** A-10: primero la página de ids (sin joins) y luego se cargan solo esos
     *  pedidos con líneas+extras en un único fetch. Evita el N+1 de toSPI y la
     *  paginación en memoria de la carga de colecciones. */
    @Query("SELECT p.id FROM PedidoCliente p WHERE p.clienteId = :clienteId")
    Page<Long> findIdsByClienteId(@Param("clienteId") Long clienteId, Pageable pageable);

    @EntityGraph(attributePaths = {"lineas", "lineas.extras"})
    @Query("SELECT p FROM PedidoCliente p WHERE p.id IN :ids")
    List<PedidoCliente> findByIdsWithLineas(@Param("ids") Collection<Long> ids);

    default Page<PedidoCliente> historialPaginado(Long clienteId, Pageable pageable) {
        Page<Long> ids = findIdsByClienteId(clienteId, pageable);
        if (ids.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, ids.getTotalElements());
        }
        Map<Long, PedidoCliente> porId = findByIdsWithLineas(ids.getContent()).stream()
                .collect(Collectors.toMap(PedidoCliente::getId, Function.identity()));
        List<PedidoCliente> ordenados = ids.getContent().stream()
                .map(porId::get)
                .toList();
        return new PageImpl<>(ordenados, pageable, ids.getTotalElements());
    }
}
