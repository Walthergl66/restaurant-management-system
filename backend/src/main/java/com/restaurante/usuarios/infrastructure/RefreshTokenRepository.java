package com.restaurante.usuarios.infrastructure;

import com.restaurante.usuarios.domain.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /** A-03: consumo atómico del token de un solo uso. Solo una transacción
     *  consigue fila afectada (revoked false -> true); las demás devuelven 0. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE RefreshToken t SET t.revoked = true WHERE t.tokenHash = :tokenHash AND t.revoked = false")
    int consumirActivoSiExiste(@Param("tokenHash") String tokenHash);

    /** A-03/A-04: revoca todas las sesiones activas del usuario (política de
     *  familia ante reutilización y cambios administrativos). */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE RefreshToken t SET t.revoked = true WHERE t.usuario.id = :usuarioId AND t.revoked = false")
    int revocarActivasDe(@Param("usuarioId") Long usuarioId);

    long deleteByUsuarioIdAndRevokedTrue(Long usuarioId);
}