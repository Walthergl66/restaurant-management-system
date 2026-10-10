package com.restaurante.usuarios.application;

import com.restaurante.shared.domain.exception.BusinessRuleException;
import com.restaurante.shared.domain.exception.NotFoundException;
import com.restaurante.shared.domain.exception.UnauthorizedException;
import com.restaurante.usuarios.domain.RecuperacionPassword;
import com.restaurante.usuarios.domain.RefreshToken;
import com.restaurante.usuarios.domain.Usuario;
import com.restaurante.usuarios.infrastructure.RecuperacionPasswordRepository;
import com.restaurante.usuarios.infrastructure.RefreshTokenRepository;
import com.restaurante.usuarios.infrastructure.UsuarioRepository;
import com.restaurante.usuarios.infrastructure.security.JwtProperties;
import com.restaurante.usuarios.infrastructure.security.JwtService;
import com.restaurante.usuarios.web.dto.AuthResponse;
import com.restaurante.usuarios.web.dto.LoginRequest;
import com.restaurante.usuarios.web.dto.UsuarioInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Autenticación y emisión/renovación de tokens.
 */
@Service
@Transactional
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    /** Vigencia del token de recuperación de contraseña. */
    private static final Duration VIGENCIA_RECUPERACION = Duration.ofMinutes(30);

    private final UsuarioRepository usuarioRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final RecuperacionPasswordRepository recuperacionRepository;
    private final RefreshTokenFamiliaRevoker familiaRevoker;
    private final UsuarioService usuarioService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;

    public AuthService(UsuarioRepository usuarioRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       RecuperacionPasswordRepository recuperacionRepository,
                       RefreshTokenFamiliaRevoker familiaRevoker,
                       UsuarioService usuarioService,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       JwtProperties jwtProperties) {
        this.usuarioRepository = usuarioRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.recuperacionRepository = recuperacionRepository;
        this.familiaRevoker = familiaRevoker;
        this.usuarioService = usuarioService;
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

    /**
     * Registro público de un cliente (RF-45). Crea el usuario con rol CLIENTE y
     * lo deja con sesión iniciada (mismo contrato que el login). Recibe datos
     * sueltos para no acoplar application a los DTO de web (guarda ArchUnit).
     */
    public AuthResponse registro(String username, String nombre, String password) {
        Usuario usuario = usuarioService.registrarCliente(username, password, nombre);
        return emitirTokens(usuario);
    }

    /**
     * RF-45: solicita un token de recuperación. No revela si el usuario existe
     * (respuesta uniforme). No hay servicio de correo en este alcance, así que
     * el token se registra en el log del servidor para entrega manual; en
     * producción se reemplaza por el envío por email.
     */
    public void solicitarRecuperacion(String username) {
        Optional<Usuario> posible = usuarioRepository.findByUsernameAndActivoTrue(username);
        if (posible.isEmpty()) {
            return;
        }
        Usuario usuario = posible.get();
        String token = UUID.randomUUID().toString();
        RecuperacionPassword recuperacion = new RecuperacionPassword(
                usuario.getId(), RecuperacionPassword.hash(token), Instant.now().plus(VIGENCIA_RECUPERACION));
        recuperacionRepository.save(recuperacion);
        log.warn("Recuperación de contraseña solicitada para '{}'. Token válido {} min (entrega manual): {}",
                username, VIGENCIA_RECUPERACION.toMinutes(), token);
    }

    /**
     * RF-45: restablece la contraseña con un token de un solo uso válido. Al
     * cambiarla se revocan todas las sesiones activas (refresh + versión).
     */
    public void restablecerPassword(String token, String nuevaPassword) {
        RecuperacionPassword recuperacion = recuperacionRepository
                .findByTokenHash(RecuperacionPassword.hash(token))
                .orElseThrow(() -> new BusinessRuleException("Token de recuperación inválido"));
        if (recuperacion.isUsado() || recuperacion.isExpirado()) {
            throw new BusinessRuleException("El token de recuperación está vencido o ya fue usado");
        }
        Usuario usuario = usuarioRepository.findById(recuperacion.getUsuarioId())
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));
        usuario.cambiarPasswordHash(passwordEncoder.encode(nuevaPassword));
        usuario.incrementarSesionVersion();
        refreshTokenRepository.revocarActivasDe(usuario.getId());
        recuperacion.marcarUsado();
        recuperacionRepository.save(recuperacion);
    }

    public AuthResponse refresh(String rawToken) {
        UUID uuid = parseUuid(rawToken);
        String hash = RefreshToken.hash(uuid);
        RefreshToken refreshToken = refreshTokenRepository
                .findByTokenHash(hash)
                .orElseThrow(() -> new UnauthorizedException("Token de refresco inválido"));

        // Reutilización de un token ya consumido: posible robo o carrera perdida.
        // La revocación de la familia persiste en transacción propia (el 401
        // vuelca la transacción exterior).
        if (refreshToken.isRevoked()) {
            familiaRevoker.revocarFamiliaDe(refreshToken.getUsuario().getId());
            throw new UnauthorizedException("Token de refresco vencido o revocado");
        }
        if (refreshToken.isExpired()) {
            throw new UnauthorizedException("Token de refresco vencido o revocado");
        }
        Usuario usuario = refreshToken.getUsuario();
        if (!usuario.isActivo()) {
            throw new UnauthorizedException("El usuario está desactivado");
        }

        // A-03: consumo atómico en la base. Si otra transacción rotó el token
        // justo antes (carrera), el UPDATE afecta 0 filas y esta petición pierde.
        if (refreshTokenRepository.consumirActivoSiExiste(hash) != 1) {
            familiaRevoker.revocarFamiliaDe(usuario.getId());
            throw new UnauthorizedException("Token de refresco vencido o revocado");
        }

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
                usuario.getUsername(), usuario.getRol().getCodigo(), usuario.getPermisoCodigos(),
                usuario.getSesionVersion());
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