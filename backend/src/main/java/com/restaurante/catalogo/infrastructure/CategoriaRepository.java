package com.restaurante.catalogo.infrastructure;

import com.restaurante.catalogo.domain.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    boolean existsByNombreIgnoreCase(String nombre);

    List<Categoria> findByActivoTrueOrderByOrdenAscNombreAsc();
}