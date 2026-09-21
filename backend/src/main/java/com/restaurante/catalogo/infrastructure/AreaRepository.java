package com.restaurante.catalogo.infrastructure;

import com.restaurante.catalogo.domain.Area;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AreaRepository extends JpaRepository<Area, Long> {

    boolean existsByNombreIgnoreCase(String nombre);

    List<Area> findByActivoTrueOrderByNombreAsc();
}