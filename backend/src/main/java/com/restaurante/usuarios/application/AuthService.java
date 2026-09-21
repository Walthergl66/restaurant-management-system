package com.restaurante.usuarios.application;

import com.restaurante.shared.domain.exception.NotFoundException;
import com.restaurante.shared.domain.exception.UnauthorizedException;
import com.restaurante.usuarios.domain.RefreshToken;
import com.restaurante.usuarios.domain.Usuario;
import com.restaurante.usuarios.infrastructure.RefreshTokenRepository;
import com.restaurante.usuarios.infrastructure.UsuarioRepository;
import com.restaurante.usuarios.infrastructure.security.JwtProperties;
import com.restaurante.usuarios.infrastructure.security.JwtService;
import com.restaurante.usuarios.web.dto.AuthResponse;
import com.restaurante.usuarios.web.dto.LoginRequest;
import com.restaurante.usuarios.web.dto.UsuarioInfo;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Autenticación y emisión/renovación de tokens.
 */
@Service
@Transactional
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;

    public AuthService(UsuarioRepository usuarioRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       JwtProperties jwtProperties) {
        this.usuarioRepository = usuarioRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.jwtProperties = jwtProperties;
    }

    public AuthResponse login(LoginRequest request) {
        Usuario usuario = usuarioRepository.findByUsernameAndActivoTrue(request.username())
                .orElseThrow(() -> new UnauthorizedException("Usuario o contraseña incorrectos"));
        if (!passwordEncoder.matches(request.password(), usuario.getPasswordHash())) {
            throw new UnauthorizedException("Usuario o contraseña incorrectos");
        }
        return emitirTokens(usuario);
    }

    public AuthResponse refresh(String rawToken) {
        RefreshToken refreshToken = refreshTokenRepository
                .findByTokenHash(RefreshToken.hash(parseUuid(rawToken)))
                .orElseThrow(() -> new UnauthorizedException("Token de refresco inválido"));
        if (refreshToken.isRevoked() || refreshToken.isExpired()) {
            throw new UnauthorizedException("Token de refresco vencido o revocado");
        }
        Usuario usuario = refreshToken.getUsuario();
        if (!usuario.isActivo()) {
            throw new UnauthorizedException("El usuario está desactivado");
        }
        refreshToken.revocar();

        String nuevoRefresh = generarYGuardarRefreshToken(usuario);
        return emitirAcceso(usuario, nuevoRefresh);
    }

    public void logout(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }
        refreshTokenRepository.findByTokenHash(RefreshToken.hash(parseUuid(rawToken)))
                .ifPresent(RefreshToken::revocar);
    }

    @Transactional(readOnly = true)
    public UsuarioInfo quienSoy(String username) {
        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));
        return UsuarioInfo.from(usuario);
    }

    private AuthResponse emitirTokens(Usuario usuario) {
        String refreshToken = generarYGuardarRefreshToken(usuario);
        return emitirAcceso(usuario, refreshToken);
    }

    private AuthResponse emitirAcceso(Usuario usuario, String refreshToken) {
        String accessToken = jwtService.generarAcceso(
                usuario.getUsername(), usuario.getRol().getCodigo(), usuario.getPermisoCodigos());
        long expiresInSeconds = jwtProperties.getExpiration().toSeconds();
        return AuthResponse.of(accessToken, refreshToken, expiresInSeconds, UsuarioInfo.from(usuario));
    }

    private String generarYGuardarRefreshToken(Usuario usuario) {
        UUID token = UUID.randomUUID();
        RefreshToken entity = new RefreshToken(usuario, RefreshToken.hash(token),
                Instant.now().plus(jwtProperties.getRefreshExpiration()));
        refreshTokenRepository.save(entity);
        return token.toString();
    }

    private static UUID parseUuid(String rawToken) {
        try {
            return UUID.fromString(rawToken);
        } catch (IllegalArgumentException e) {
            throw new UnauthorizedException("Token de refresco inválido");
        }
    }
}