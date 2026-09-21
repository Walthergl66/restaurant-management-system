package com.restaurante.anulaciones.infrastructure;

import com.restaurante.anulaciones.domain.Anulacion;
import com.restaurante.anulaciones.domain.EstadoAnulacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface AnulacionRepository extends JpaRepository<Anulacion, Long> {

    List<Anulacion> findByPedidoCodigo(String pedidoCodigo);

    List<Anulacion> findByPedidoCodigoIn(List<String> pedidoCodigos);

    /**
     * Anulaciones en un estado dentro del período, para conciliar reportes
     * (RF-51). El filtro va en la base de datos, no en memoria.
     */
    List<Anulacion> findByCreatedAtBetweenAndEstado(Instant desde, Instant hasta,
                                                    EstadoAnulacion estado);

    /**
     * Cantidad total ya aprobada para una línea; valida que la anulación nunca
     * exceda lo pedido en la línea original.
     */
    @Query("SELECT COALESCE(SUM(a.cantidad), 0) FROM Anulacion a "
            + "WHERE a.pedidoCodigo = :pedidoCodigo AND a.lineaId = :lineaId "
            + "AND a.estado = com.restaurante.anulaciones.domain.EstadoAnulacion.APROBADA")
    int cantidadAprobada(@Param("pedidoCodigo") String pedidoCodigo, @Param("lineaId") Long lineaId);
}