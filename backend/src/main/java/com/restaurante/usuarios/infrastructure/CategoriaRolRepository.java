package com.restaurante.usuarios.infrastructure;

import com.restaurante.usuarios.domain.CategoriaRol;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CategoriaRolRepository extends JpaRepository<CategoriaRol, Long> {

    Optional<CategoriaRol> findByCodigo(String codigo);
}
