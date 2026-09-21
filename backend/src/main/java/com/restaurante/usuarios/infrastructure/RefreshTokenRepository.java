package com.restaurante.usuarios.infrastructure;

import com.restaurante.usuarios.domain.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    long deleteByUsuarioIdAndRevokedTrue(Long usuarioId);
}