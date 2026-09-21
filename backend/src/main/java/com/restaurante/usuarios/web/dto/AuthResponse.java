package com.restaurante.usuarios.web.dto;

/**
 * Resultado de iniciar sesión o renovar el token.
 */
public record AuthResponse(
        String accessToken,
        String refreshToken,
        long expiresInSeconds,
        String tokenType,
        UsuarioInfo usuario) {

    public static AuthResponse of(String accessToken, String refreshToken, long expiresInSeconds,
                                  UsuarioInfo usuario) {
        return new AuthResponse(accessToken, refreshToken, expiresInSeconds, "Bearer", usuario);
    }
}