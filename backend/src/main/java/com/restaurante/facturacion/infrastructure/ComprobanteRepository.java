package com.restaurante.facturacion.infrastructure;

import com.restaurante.facturacion.domain.Comprobante;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;

public interface ComprobanteRepository extends JpaRepository<Comprobante, Long> {

    @Query(value = "SELECT nextval('seq_correlativo_comprobante')", nativeQuery = true)
    Long siguienteSecuencial();

    List<Comprobante> findByFechaBetween(Instant desde, Instant hasta);

    List<Comprobante> findByCuentaId(Long cuentaId);

    List<Comprobante> findTop100ByOrderByIdDesc();

    @Query("select c from Comprobante c order by c.secuencial desc limit 1")
    List<Comprobante> ultimos(int limite);
}