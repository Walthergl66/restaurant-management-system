package com.restaurante.usuarios.application;

import com.restaurante.shared.domain.exception.BusinessRuleException;
import com.restaurante.shared.domain.exception.ConflictException;
import com.restaurante.shared.domain.exception.NotFoundException;
import com.restaurante.usuarios.ClienteAutoRegistrado;
import com.restaurante.usuarios.Usuarios;
import com.restaurante.usuarios.domain.Permiso;
import com.restaurante.usuarios.domain.Rol;
import com.restaurante.usuarios.domain.RolCodigo;
import com.restaurante.usuarios.domain.Usuario;
import com.restaurante.usuarios.infrastructure.CategoriaRolRepository;
import com.restaurante.usuarios.infrastructure.PermisoRepository;
import com.restaurante.usuarios.infrastructure.RefreshTokenRepository;
import com.restaurante.usuarios.infrastructure.RolRepository;
import com.restaurante.usuarios.infrastructure.UsuarioRepository;
import com.restaurante.usuarios.web.dto.ActualizarEstadoRequest;
import com.restaurante.usuarios.web.dto.ActualizarUsuarioRequest;
import com.restaurante.usuarios.web.dto.CambiarPasswordRequest;
import com.restaurante.usuarios.web.dto.CategoriaRolDto;
import com.restaurante.usuarios.web.dto.CrearUsuarioRequest;
import com.restaurante.usuarios.web.dto.PermisoDto;
import com.restaurante.usuarios.web.dto.RolDto;
import com.restaurante.usuarios.web.dto.TaxonomiaRolesDto;
import com.restaurante.usuarios.web.dto.UsuarioAdminResponse;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Administración de usuarios, roles y contraseñas.
 */
@Service
@Transactional
public class UsuarioService implements Usuarios {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final CategoriaRolRepository categoriaRolRepository;
    private final PermisoRepository permisoRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;
    private final com.restaurante.auditoria.Auditoria auditoriaService;
    private final tools.jackson.databind.ObjectMapper objectMapper;

    public UsuarioService(UsuarioRepository usuarioRepository,
                          RolRepository rolRepository,
                          CategoriaRolRepository categoriaRolRepository,
                          PermisoRepository permisoRepository,
                          RefreshTokenRepository refreshTokenRepository,
                          PasswordEncoder passwordEncoder,
                          ApplicationEventPublisher eventPublisher,
                          com.restaurante.auditoria.Auditoria auditoriaService,
                          tools.jackson.databind.ObjectMapper objectMapper) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.categoriaRolRepository = categoriaRolRepository;
        this.permisoRepository = permisoRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.eventPublisher = eventPublisher;
        this.auditoriaService = auditoriaService;
        this.objectMapper = objectMapper;
    }

    public UsuarioAdminResponse crear(CrearUsuarioRequest request) {
        String username = normalizarUsername(request.username());
        if (usuarioRepository.existsByUsername(username)) {
            throw new ConflictException("Ya existe un usuario con el nombre '" + username + "'");
        }
        Rol rol = rolRepository.findByCodigo(request.rolCodigo())
                .orElseThrow(() -> new NotFoundException("Rol no encontrado: " + request.rolCodigo()));
        if (!rol.isActivo()) {
            throw new BusinessRuleException("El rol está desactivado");
        }
        exigirRolAsignable(rol);
        Usuario usuario = new Usuario(
                username,
                passwordEncoder.encode(request.password()),
                request.nombre().trim(),
                rol);
        if (request.permisos() != null && !request.permisos().isEmpty()) {
            if (!rol.isConfigurable()) {
                throw new BusinessRuleException(
                        "El rol " + rol.getCodigo() + " no admite permisos personalizados: nace con sus funcionalidades por defecto");
            }
            usuario.asignarPermisosExplicitos(resolverPermisos(request.permisos()));
        }
        Usuario guardado = usuarioRepository.save(usuario);
        try {
            auditoriaService.registrar(usuarioActual(), "USUARIO_CREADO", "USUARIO",
                    guardado.getUsername(), json(Map.of("rol", rol.getCodigo(), "nombre", guardado.getNombre())), "USUARIOS");
        } catch (Exception ignored) {
        }
        return UsuarioAdminResponse.from(guardado);
    }

    /**
     * Auto-registro público de un cliente (RF-45): crea el usuario con el rol
     * CLIENTE, pendiente de verificar el correo, y publica (en la misma
     * transacción) el evento para que el módulo clientes materialice el perfil
     * con la cédula y el celular reales capturados en el registro.
     */
    public Usuario registrarCliente(String username, String rawPassword, String nombre,
                                    String cedula, String celular) {
        String limpio = normalizarUsername(username);
        if (limpio == null || limpio.isBlank()) {
            throw new BusinessRuleException("El nombre de usuario es obligatorio");
        }
        if (usuarioRepository.existsByUsername(limpio)) {
            throw new ConflictException("Ya existe un usuario con el nombre '" + limpio + "'");
        }
        Rol rol = rolRepository.findByCodigo(RolCodigo.CLIENTE)
                .orElseThrow(() -> new NotFoundException("Rol no encontrado: " + RolCodigo.CLIENTE));
        if (!rol.isActivo()) {
            throw new BusinessRuleException("El rol está desactivado");
        }
        Usuario usuario = new Usuario(limpio, passwordEncoder.encode(rawPassword), nombre.trim(), rol);
        usuario.requiereVerificacionEmail();
        Usuario guardado = usuarioRepository.save(usuario);
        eventPublisher.publishEvent(new ClienteAutoRegistrado(
                guardado.getId(), nombre.trim(), cedula.trim(), celular.trim()));
        return guardado;
    }

    /**
     * Auto-registro público de un repartidor (rol DELIVERY): crea la cuenta
     * con su celular de contacto, pendiente de verificar el correo.
     *
     * <p>El rol DELIVERY no se asigna desde el panel (el administrador solo
     * lo gestiona), así que este es el único camino para que un repartidor
     * tenga cuenta. A diferencia del cliente, no publica evento: el
     * repartidor no tiene perfil en el módulo de clientes.
     */
    public Usuario registrarRepartidor(String username, String rawPassword,
                                       String nombre, String celular) {
        String limpio = normalizarUsername(username);
        if (limpio == null || limpio.isBlank()) {
            throw new BusinessRuleException("El nombre de usuario es obligatorio");
        }
        if (usuarioRepository.existsByUsername(limpio)) {
            throw new ConflictException("Ya existe un usuario con el nombre '" + limpio + "'");
        }
        Rol rol = rolRepository.findByCodigo(RolCodigo.DELIVERY)
                .orElseThrow(() -> new NotFoundException("Rol no encontrado: " + RolCodigo.DELIVERY));
        if (!rol.isActivo()) {
            throw new BusinessRuleException("El rol está desactivado");
        }
        Usuario usuario = new Usuario(limpio, passwordEncoder.encode(rawPassword), nombre.trim(), rol);
        usuario.asignarCelular(celular);
        usuario.requiereVerificacionEmail();
        return usuarioRepository.save(usuario);
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
            if (!rol.isActivo()) {
                throw new BusinessRuleException("El rol está desactivado");
            }
            exigirRolAsignable(rol);
            usuario.asignarRol(rol);
            // El set explícito quedó armado sobre el rol anterior: al cambiar de
            // rol se limpia para que rijan los defaults del nuevo rol.
            usuario.asignarPermisosExplicitos(Set.of());
            credencialesCambiadas = true;
        }
        if (request.permisos() != null) {
            if (!usuario.getRol().isConfigurable()) {
                throw new BusinessRuleException(
                        "El rol " + usuario.getRol().getCodigo() + " no admite permisos personalizados");
            }
            if (request.permisos().isEmpty()) {
                throw new BusinessRuleException("El usuario debe conservar al menos una funcionalidad");
            }
            usuario.asignarPermisosExplicitos(resolverPermisos(request.permisos()));
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

    /**
     * El administrador solo puede crear y asignar los roles marcados como
     * asignables en la base de datos: ADMIN tiene una sola cuenta y las
     * cuentas de CLIENTE y DELIVERY nacen por auto-registro.
     */
    private void exigirRolAsignable(Rol rol) {
        if (rol.isAsignablePorAdmin()) {
            return;
        }
        String motivo = "AUTO_REGISTRO".equals(rol.getOrigen())
                ? "las cuentas con ese rol nacen por auto-registro"
                : "hay una sola cuenta con ese rol";
        throw new BusinessRuleException(
                "El rol " + rol.getCodigo() + " no se asigna desde el panel: " + motivo);
    }

    /**
     * Valida los códigos de permiso recibidos y devuelve las entidades.
     * Un código desconocido o desactivado se rechaza completo (409 de regla
     * de negocio) para no guardar un permiso que nadie va a poder usar.
     */
    private Set<Permiso> resolverPermisos(List<String> codigos) {
        List<Permiso> encontrados = permisoRepository.findByCodigoIn(codigos);
        Set<String> encontradosCodigos = encontrados.stream()
                .filter(Permiso::isActivo)
                .map(Permiso::getCodigo)
                .collect(java.util.stream.Collectors.toSet());
        Set<String> invalidos = codigos.stream()
                .map(String::trim)
                .filter(c -> !encontradosCodigos.contains(c))
                .collect(java.util.stream.Collectors.toSet());
        if (!invalidos.isEmpty()) {
            throw new BusinessRuleException("Permisos inválidos: " + String.join(", ", invalidos));
        }
        return Set.copyOf(encontrados);
    }

    public void cambiarPassword(Long id, CambiarPasswordRequest request) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));
        usuario.cambiarPasswordHash(passwordEncoder.encode(request.nuevaPassword()));
        invalidarSesiones(usuario);
    }

    /**
     * Borrado definitivo del usuario (distinto de la baja lógica vía
     * {@link #actualizar}). Borra la fila: refresh tokens, verificaciones y
     * recuperaciones caen en cascada. Si el usuario tiene perfil de cliente
     * (FK en {@code comercial.clientes}, sin cascada) no se puede borrar.
     */
    public void eliminar(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));
        String username = usuario.getUsername();
        try {
            usuarioRepository.delete(usuario);
            usuarioRepository.flush();
            try {
                auditoriaService.registrar(usuarioActual(), "USUARIO_ELIMINADO", "USUARIO",
                        username, "{\"accion\":\"ELIMINADO_DEFINITIVO\"}", "USUARIOS");
            } catch (Exception ignored) {
            }
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("No se puede eliminar al usuario: tiene un perfil de cliente asociado");
        }
    }

    /** Activación/desactivación directa desde la lista, con las mismas
     *  garantías de revocación de sesiones que una edición completa. */
    public UsuarioAdminResponse cambiarEstado(Long id, ActualizarEstadoRequest request) {
        Usuario usuario = usuarioRepository.findByIdWithRol(id)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));
        if (request.activo()) {
            usuario.activar();
        } else {
            usuario.desactivar();
        }
        invalidarSesiones(usuario);
        return UsuarioAdminResponse.from(usuario);
    }

    /**
     * Taxonomía de roles para el panel: las categorías con los roles que
     * contienen y la lista plana de todos ellos. Solo roles activos.
     */
    @Transactional(readOnly = true)
    public TaxonomiaRolesDto listarTaxonomiaRoles() {
        List<RolDto> roles = rolRepository.findAll().stream()
                .filter(Rol::isActivo)
                .sorted(Comparator
                        .comparing((Rol r) -> r.getCategoria().getCodigo())
                        .thenComparing(Rol::getCodigo))
                .map(this::aDtoRol)
                .toList();
        List<CategoriaRolDto> categorias = categoriaRolRepository.findAll().stream()
                .sorted(Comparator.comparing(c -> c.getCodigo()))
                .map(c -> new CategoriaRolDto(
                        c.getCodigo(),
                        c.getDescripcion(),
                        roles.stream()
                                .filter(r -> r.categoria().equals(c.getCodigo()))
                                .toList()))
                .toList();
        return new TaxonomiaRolesDto(categorias, roles);
    }

    private RolDto aDtoRol(Rol rol) {
        return new RolDto(
                rol.getId(),
                rol.getCodigo(),
                rol.getDescripcion(),
                rol.getCategoria().getCodigo(),
                rol.getCategoria().getDescripcion(),
                rol.isAsignablePorAdmin(),
                rol.isConfigurable(),
                rol.getOrigen(),
                rol.getPermisoCodigos());
    }

    /** Catálogo completo de permisos para el checklist del panel. */
    @Transactional(readOnly = true)
    public List<PermisoDto> listarPermisos() {
        return permisoRepository.findAll().stream()
                .filter(Permiso::isActivo)
                .map(p -> new PermisoDto(p.getCodigo(), p.getModulo(), p.getDescripcion()))
                .sorted(java.util.Comparator.comparing(PermisoDto::modulo)
                        .thenComparing(PermisoDto::codigo))
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
        return usuarioRepository.findByUsername(normalizarUsername(username)).map(u -> resumo(u));
    }

    @Override
    @Transactional
    public Optional<UsuarioResumen> porUsernameBloqueado(String username) {
        return usuarioRepository.findBloqueadoPorUsername(normalizarUsername(username)).map(u -> resumo(u));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UsuarioResumen> porId(Long id) {
        return usuarioRepository.findById(id).map(u -> resumo(u));
    }

    @Override
    @Transactional
    public void cambiarCorreo(Long usuarioId, String nuevoCorreo) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado: " + usuarioId));
        String limpio = nuevoCorreo == null ? null : nuevoCorreo.trim().toLowerCase();
        if (limpio == null || limpio.isBlank()) {
            throw new BusinessRuleException("El correo no puede estar vacío");
        }
        if (!limpio.equals(usuario.getUsername())) {
            if (usuarioRepository.existsByUsername(limpio)) {
                throw new ConflictException("Ya existe un usuario con el correo '" + limpio + "'");
            }
            usuario.cambiarCorreo(limpio);
        }
    }

    private UsuarioResumen resumo(Usuario u) {
        return new UsuarioResumen(
                u.getId(),
                u.getUsername(),
                u.getNombre(),
                u.getRol() != null ? u.getRol().getCodigo() : null,
                u.isActivo());
    }

    private String usuarioActual() {
        try {
            org.springframework.security.core.Authentication auth =
                    org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || auth.getPrincipal() == null
                    || "anonymousUser".equals(auth.getPrincipal()) || "sistema".equals(auth.getPrincipal())) {
                return "sistema";
            }
            return auth.getName();
        } catch (Exception e) {
            return "sistema";
        }
    }

    private String json(Map<String, String> datos) {
        try {
            return objectMapper.writeValueAsString(datos);
        } catch (Exception e) {
            return "{\"detalle\":\"no serializable\"}";
        }
    }

    private String normalizarUsername(String username) {
        return username == null ? null : username.trim().toLowerCase(Locale.ROOT);
    }
}
