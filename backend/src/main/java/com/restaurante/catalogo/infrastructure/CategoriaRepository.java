package com.restaurante.catalogo.infrastructure;

import com.restaurante.catalogo.domain.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    // Incluye registros inactivos: desactivar no libera el nombre.
    @Query("select count(e) > 0 from Categoria e where lower(trim(function('regexp_replace', e.nombre, '[[:space:]]+', ' ', 'g'))) = lower(trim(:nombre))")
    boolean existsByNombreIgnoreCase(@Param("nombre") String nombre);

    @Query("select count(e) > 0 from Categoria e where lower(trim(function('regexp_replace', e.nombre, '[[:space:]]+', ' ', 'g'))) = lower(trim(:nombre)) and e.id <> :id")
    boolean existsByNombreIgnoreCaseAndIdNot(@Param("nombre") String nombre, @Param("id") Long id);

    List<Categoria> findByActivoTrueOrderByOrdenAscNombreAsc();
}
