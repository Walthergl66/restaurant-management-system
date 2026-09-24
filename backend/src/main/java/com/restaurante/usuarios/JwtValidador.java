package com.restaurante.usuarios;

import java.util.List;
import java.util.Optional;

/**
 * SPI público del módulo usuarios para validar access tokens JWT. Lo consume el
 * módulo clientes para autenticar el handshake/STOMP del canal en vivo (RF-43)
 * sin romper el encapsulamiento de Spring Modulith. Además de firma y vigencia,
 * verifica en base que la versión de sesión del usuario coincida (A-04).
 */
public interface JwtValidador {

    /**
     * @param bearerToken cabecera {@code Authorization} completa (o {@code null}).
     * @return acceso válido con nombre, rol y permisos; vacío si es inválido.
     */
    Optional<AccesoValido> validar(String bearerToken);

    record AccesoValido(String username, String rol, List<String> permisos) {
    }
}