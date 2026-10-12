package com.restaurante.catalogo.infrastructure;

import com.restaurante.catalogo.domain.Categoria;
import com.restaurante.catalogo.domain.Producto;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductoRepository extends JpaRepository<Producto, Long> {

    // Incluye registros inactivos: desactivar no libera el nombre.
    @Query("select count(e) > 0 from Producto e where lower(trim(function('regexp_replace', e.nombre, '[[:space:]]+', ' ', 'g'))) = lower(trim(:nombre))")
    boolean existsByNombreIgnoreCase(@Param("nombre") String nombre);

    @Query("select count(e) > 0 from Producto e where lower(trim(function('regexp_replace', e.nombre, '[[:space:]]+', ' ', 'g'))) = lower(trim(:nombre)) and e.id <> :id")
    boolean existsByNombreIgnoreCaseAndIdNot(@Param("nombre") String nombre, @Param("id") Long id);

    /**
     * Catálogo visible para el menú público: productos activos con sus extras
     * e ingredientes removibles, listos para serializar.
     */
    @EntityGraph(attributePaths = {"categoria", "area", "extras", "ingredientes"})
    List<Producto> findByActivoTrue();

    @EntityGraph(attributePaths = {"extras", "ingredientes"})
    List<Producto> findByCategoria(Categoria categoria);
}
