package com.restaurante.usuarios.web;

import com.restaurante.usuarios.application.AuthService;
import com.restaurante.usuarios.web.dto.AuthResponse;
import com.restaurante.usuarios.web.dto.LoginRequest;
import com.restaurante.usuarios.web.dto.RefreshTokenRequest;
import com.restaurante.usuarios.web.dto.UsuarioInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

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

    @PostMapping("/refresh")
    @Operation(summary = "Renueva el token de acceso con un token de refresco")
    public AuthResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return authService.refresh(request.refreshToken());
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