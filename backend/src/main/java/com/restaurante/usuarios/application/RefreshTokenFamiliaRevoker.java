package com.restaurante.usuarios.application;

import com.restaurante.usuarios.infrastructure.RefreshTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** A-03: revocación de la familia de sesiones en transacción propia
 *  (REQUIRES_NEW). Se invoca justo antes de rechazar una reutilización de token:
 *  debe persistir aunque la transacción exterior (que lanza el 401) se revierta. */
@Service
public class RefreshTokenFamiliaRevoker {

    private final RefreshTokenRepository refreshTokenRepository;

    public RefreshTokenFamiliaRevoker(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void revocarFamiliaDe(Long usuarioId) {
        refreshTokenRepository.revocarActivasDe(usuarioId);
    }
}