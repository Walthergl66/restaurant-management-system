package com.restaurante.usuarios.infrastructure;

import com.restaurante.usuarios.domain.VerificacionEmail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VerificacionEmailRepository extends JpaRepository<VerificacionEmail, Long> {

    /** Código de verificación pendiente más reciente de un usuario. */
    Optional<VerificacionEmail> findTopByUsuarioIdAndUsadoFalseOrderByIdDesc(Long usuarioId);
}
