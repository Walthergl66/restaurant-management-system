package com.restaurante.mesas.infrastructure;

import com.restaurante.mesas.domain.Zona;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ZonaRepository extends JpaRepository<Zona, Long> {

    List<Zona> findAllByOrderByOrdenAscIdAsc();
}
