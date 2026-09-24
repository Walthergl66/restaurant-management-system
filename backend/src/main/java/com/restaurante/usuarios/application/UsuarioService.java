package com.restaurante.usuarios.application;

import com.restaurante.shared.domain.exception.BusinessRuleException;
import com.restaurante.shared.domain.exception.ConflictException;
import com.restaurante.shared.domain.exception.NotFoundException;
import com.restaurante.usuarios.Usuarios;
import com.restaurante.usuarios.domain.Rol;
import com.restaurante.usuarios.domain.Usuario;
import com.restaurante.usuarios.infrastructure.PermisoRepository;
import com.restaurante.usuarios.infrastructure.RefreshTokenRepository;
import com.restaurante.usuarios.infrastructure.RolRepository;
import com.restaurante.usuarios.infrastructure.UsuarioRepository;
import com.restaurante.usuarios.web.dto.ActualizarUsuarioRequest;
import com.restaurante.usuarios.web.dto.CambiarPasswordRequest;
import com.restaurante.usuarios.web.dto.CrearUsuarioRequest;
import com.restaurante.usuarios.web.dto.RolDto;
import com.restaurante.usuarios.web.dto.UsuarioAdminResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Administración de usuarios, roles y contraseñas.
 */
@Service
@Transactional
public class UsuarioService implements Usuarios {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PermisoRepository permisoRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository,
                          RolRepository rolRepository,
                          PermisoRepository permisoRepository,
                          RefreshTokenRepository refreshTokenRepository,
                          PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.permisoRepository = permisoRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public UsuarioAdminResponse crear(CrearUsuarioRequest request) {
        if (usuarioRepository.existsByUsername(request.username())) {
            throw new ConflictException("Ya existe un usuario con el nombre '" + request.username() + "'");
        }
        Rol rol = rolRepository.findByCodigo(request.rolCodigo())
                .orElseThrow(() -> new NotFoundException("Rol no encontrado: " + request.rolCodigo()));
        if (!rol.isActivo()) {
            throw new BusinessRuleException("El rol está desactivado");
        }
        Usuario usuario = new Usuario(
                request.username().trim(),
                passwordEncoder.encode(request.password()),
                request.nombre().trim(),
                rol);
        return UsuarioAdminResponse.from(usuarioRepository.save(usuario));
    }

    @Transactional(readOnly = true)
    public Page<UsuarioAdminResponse> listar(Pageable pageable) {
        return usuarioRepository.findAll(pageable).map(UsuarioAdminResponse::from);
    }

    @Transactional(readOnly = true)
    public UsuarioAdminResponse obtener(Long id) {
        return UsuarioAdminResponse.from(usuarioRepository.findByIdWithRol(id)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado")));
    }

    public UsuarioAdminResponse actualizar(Long id, ActualizarUsuarioRequest request) {
        Usuario usuario = usuarioRepository.findByIdWithRol(id)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));
        usuario.cambiarNombre(request.nombre());
        boolean credencialesCambiadas = false;
        if (request.rolCodigo() != null && !usuario.getRol().getCodigo().equals(request.rolCodigo())) {
            Rol rol = rolRepository.findByCodigo(request.rolCodigo())
                    .orElseThrow(() -> new NotFoundException("Rol no encontrado: " + request.rolCodigo()));
            usuario.asignarRol(rol);
            credencialesCambiadas = true;
        }
        if (request.activo() != null) {
            if (request.activo()) {
                usuario.activar();
            } else {
                usuario.desactivar();
            }
            credencialesCambiadas = true;
        }
        if (credencialesCambiadas) {
            invalidarSesiones(usuario);
        }
        return UsuarioAdminResponse.from(usuario);
    }

    public void cambiarPassword(Long id, CambiarPasswordRequest request) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));
        usuario.cambiarPasswordHash(passwordEncoder.encode(request.nuevaPassword()));
        invalidarSesiones(usuario);
    }

    public void eliminar(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));
        // Desactivación lógica: el historial y la auditoría exigen conservar el registro.
        usuario.desactivar();
        invalidarSesiones(usuario);
    }

    @Transactional(readOnly = true)
    public List<RolDto> listarRoles() {
        return rolRepository.findAll().stream()
                .filter(Rol::isActivo)
                .map(r -> new RolDto(r.getId(), r.getCodigo(), r.getDescripcion(), r.getPermisoCodigos()))
                .toList();
    }

    /** A-04: tras un cambio de credenciales/rol/estado se invalidan todas las
     *  sesiones: se incrementa la versión (revoca los JWT emitidos al instante
     *  vía el filtro) y se revocan los refresh tokens activos. */
    private void invalidarSesiones(Usuario usuario) {
        usuario.incrementarSesionVersion();
        refreshTokenRepository.revocarActivasDe(usuario.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UsuarioResumen> porUsername(String username) {
        return usuarioRepository.findByUsername(username)
                .map(u -> new UsuarioResumen(
                        u.getId(),
                        u.getUsername(),
                        u.getNombre(),
                        u.getRol() != null ? u.getRol().getCodigo() : null,
                        u.isActivo()));
    }
}