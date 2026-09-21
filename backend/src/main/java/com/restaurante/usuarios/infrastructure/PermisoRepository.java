package com.restaurante.usuarios.infrastructure;

import com.restaurante.usuarios.domain.Permiso;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface PermisoRepository extends JpaRepository<Permiso, Long> {

    List<Permiso> findByCodigoIn(Collection<String> codigos);
}