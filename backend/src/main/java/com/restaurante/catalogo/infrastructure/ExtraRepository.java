package com.restaurante.catalogo.infrastructure;

import com.restaurante.catalogo.domain.Extra;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExtraRepository extends JpaRepository<Extra, Long> {

    boolean existsByNombreIgnoreCase(String nombre);

    List<Extra> findByActivoTrueOrderByNombreAsc();
}