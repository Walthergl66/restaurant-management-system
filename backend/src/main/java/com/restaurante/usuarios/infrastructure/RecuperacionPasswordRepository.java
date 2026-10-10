package com.restaurante.usuarios.infrastructure;

import com.restaurante.usuarios.domain.RecuperacionPassword;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RecuperacionPasswordRepository extends JpaRepository<RecuperacionPassword, Long> {

    Optional<RecuperacionPassword> findByTokenHash(String tokenHash);
}
