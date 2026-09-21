package com.restaurante.configuracion.infrastructure;

import com.restaurante.configuracion.domain.Impresora;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ImpresoraRepository extends JpaRepository<Impresora, Long> {

    boolean existsByNombreIgnoreCase(String nombre);

    List<Impresora> findByActivoTrueOrderByNombreAsc();

    List<Impresora> findByArea(String area);
}