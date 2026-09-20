package com.restaurante.usuarios.infrastructure;

import com.restaurante.usuarios.domain.Rol;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RolRepository extends JpaRepository<Rol, Long> {

    Optional<Rol> findByCodigo(String codigo);

    boolean existsByCodigo(String codigo);
}