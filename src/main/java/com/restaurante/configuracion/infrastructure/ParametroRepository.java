package com.restaurante.configuracion.infrastructure;

import com.restaurante.configuracion.domain.Parametro;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ParametroRepository extends JpaRepository<Parametro, Long> {

    Optional<Parametro> findByClave(String clave);
}