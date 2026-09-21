package com.restaurante.catalogo.infrastructure;

import com.restaurante.catalogo.domain.Categoria;
import com.restaurante.catalogo.domain.Producto;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductoRepository extends JpaRepository<Producto, Long> {

    boolean existsByNombreIgnoreCase(String nombre);

    /**
     * Catálogo visible para el menú público: productos activos con sus extras
     * e ingredientes removibles, listos para serializar.
     */
    @EntityGraph(attributePaths = {"categoria", "area", "extras", "ingredientes"})
    List<Producto> findByActivoTrue();

    @EntityGraph(attributePaths = {"extras", "ingredientes"})
    List<Producto> findByCategoria(Categoria categoria);
}