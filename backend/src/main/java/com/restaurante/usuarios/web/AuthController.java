package com.restaurante.usuarios.web;

import com.restaurante.usuarios.application.AuthService;
import com.restaurante.usuarios.web.dto.AuthResponse;
import com.restaurante.usuarios.web.dto.LoginRequest;
import com.restaurante.usuarios.web.dto.ReenviarVerificacionRequest;
import com.restaurante.usuarios.web.dto.RefreshTokenRequest;
import com.restaurante.usuarios.web.dto.RegistroPendienteResponse;
import com.restaurante.usuarios.web.dto.RegistroRequest;
import com.restaurante.usuarios.web.dto.RestablecerPasswordRequest;
import com.restaurante.usuarios.web.dto.SolicitarRecuperacionRequest;
import com.restaurante.usuarios.web.dto.UsuarioInfo;
import com.restaurante.usuarios.web.dto.VerificarEmailRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/**
 * Autenticación: login, renovación, cierre de sesión y quién soy.
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "auth", description = "Autenticación y sesión")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @Operation(summary = "Inicia sesión y devuelve tokens de acceso y refresco")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/registro")
    @Operation(summary = "Auto-registro público de un cliente (RF-45); queda pendiente de verificar el correo")
    public ResponseEntity<RegistroPendienteResponse> registro(@Valid @RequestBody RegistroRequest request) {
        authService.registro(request.username(), request.nombre(), request.password(),
                request.cedula(), request.celular());
        RegistroPendienteResponse respuesta = new RegistroPendienteResponse(
                request.username(), false,
                "Te enviamos un código de verificación a tu correo");
        return ResponseEntity.created(URI.create("/api/v1/auth/verificar-email")).body(respuesta);
    }

    @PostMapping("/verificar-email")
    @Operation(summary = "Verifica el correo con el código (RF-45) e inicia sesión")
    public AuthResponse verificarEmail(@Valid @RequestBody VerificarEmailRequest request) {
        return authService.verificarEmail(request.username(), request.codigo());
    }

    @PostMapping("/reenviar-verificacion")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Reenvía el código de verificación al correo (RF-45)")
    public void reenviarVerificacion(@Valid @RequestBody ReenviarVerificacionRequest request) {
        authService.reenviarVerificacion(request.username());
    }

    @PostMapping("/refresh")
    @Operation(summary = "Renueva el token de acceso con un token de refresco")
    public AuthResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return authService.refresh(request.refreshToken());
    }

    @PostMapping("/solicitar-recuperacion")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Solicita un token de recuperación de contraseña (RF-45)")
    public void solicitarRecuperacion(@Valid @RequestBody SolicitarRecuperacionRequest request) {
        authService.solicitarRecuperacion(request.username());
    }

    @PostMapping("/restablecer-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Restablece la contraseña con un token de un solo uso (RF-45)")
    public void restablecerPassword(@Valid @RequestBody RestablecerPasswordRequest request) {
        authService.restablecerPassword(request.token(), request.nuevaPassword());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Revoca el token de refresco actual")
    public void logout(@Valid @RequestBody(required = false) RefreshTokenRequest request) {
        if (request != null) {
            authService.logout(request.refreshToken());
        }
    }

    @GetMapping("/me")
    @Operation(summary = "Devuelve el usuario autenticado con sus permisos")
    public UsuarioInfo me(Authentication authentication) {
        return authService.quienSoy(authentication.getName());
    }
}