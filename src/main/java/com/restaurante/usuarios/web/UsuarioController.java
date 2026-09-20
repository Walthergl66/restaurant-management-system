package com.restaurante.usuarios.web;

import com.restaurante.usuarios.PermisoCodigo;
import com.restaurante.usuarios.application.UsuarioService;
import com.restaurante.usuarios.web.dto.ActualizarUsuarioRequest;
import com.restaurante.usuarios.web.dto.CambiarPasswordRequest;
import com.restaurante.usuarios.web.dto.CrearUsuarioRequest;
import com.restaurante.usuarios.web.dto.RolDto;
import com.restaurante.usuarios.web.dto.UsuarioAdminResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Administración de usuarios y roles. Restringida a permisos del módulo
 * {@code usuarios}.
 */
@RestController
@RequestMapping("/api/v1/usuarios")
@Tag(name = "usuarios", description = "Administración de usuarios y roles")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('" + PermisoCodigo.USUARIOS_CREAR + "')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crea un usuario")
    public UsuarioAdminResponse crear(@Valid @RequestBody CrearUsuarioRequest request) {
        return usuarioService.crear(request);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('" + PermisoCodigo.USUARIOS_VER + "')")
    @Operation(summary = "Lista paginada de usuarios")
    public Page<UsuarioAdminResponse> listar(
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanio) {
        return usuarioService.listar(PageRequest.of(pagina, tamanio, Sort.by(Sort.Direction.ASC, "id")));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('" + PermisoCodigo.USUARIOS_VER + "')")
    @Operation(summary = "Obtiene un usuario por id")
    public UsuarioAdminResponse obtener(@PathVariable Long id) {
        return usuarioService.obtener(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('" + PermisoCodigo.USUARIOS_EDITAR + "')")
    @Operation(summary = "Actualiza nombre, rol o estado de un usuario")
    public UsuarioAdminResponse actualizar(@PathVariable Long id,
                                           @Valid @RequestBody ActualizarUsuarioRequest request) {
        return usuarioService.actualizar(id, request);
    }

    @PatchMapping("/{id}/password")
    @PreAuthorize("hasAuthority('" + PermisoCodigo.USUARIOS_EDITAR + "')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Cambia la contraseña de un usuario")
    public void cambiarPassword(@PathVariable Long id,
                                @Valid @RequestBody CambiarPasswordRequest request) {
        usuarioService.cambiarPassword(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('" + PermisoCodigo.USUARIOS_ELIMINAR + "')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Desactiva lógicamente un usuario")
    public void eliminar(@PathVariable Long id) {
        usuarioService.eliminar(id);
    }

    @GetMapping("/roles")
    @PreAuthorize("hasAuthority('" + PermisoCodigo.USUARIOS_VER + "')")
    @Operation(summary = "Lista roles activos con sus permisos")
    public List<RolDto> roles() {
        return usuarioService.listarRoles();
    }
}