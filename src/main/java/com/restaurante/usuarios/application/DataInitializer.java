package com.restaurante.usuarios.application;

import com.restaurante.usuarios.domain.Rol;
import com.restaurante.usuarios.domain.Usuario;
import com.restaurante.usuarios.infrastructure.RolRepository;
import com.restaurante.usuarios.infrastructure.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Crea el primer usuario administrador al arrancar cuando aún no existe ninguno.
 *
 * <p>Reglas:
 * <ul>
 *   <li>Si ya existe un usuario ADMIN no hace nada (idempotente).</li>
 *   <li>En el perfil {@code dev} usa {@code admin123} por defecto.</li>
 *   <li>En cualquier entorno respeta la variable {@code ADMIN_INITIAL_PASSWORD}.</li>
 *   <li>Si no hay contraseña disponible no se crea nada (el operador la define).</li>
 * </ul>
 */
@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;

    private final String adminInitialPassword;

    public DataInitializer(UsuarioRepository usuarioRepository,
                           RolRepository rolRepository,
                           PasswordEncoder passwordEncoder,
                           @Value("${app.inicial.admin-password:}") String adminInitialPassword) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminInitialPassword = adminInitialPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (usuarioRepository.existsByUsername("admin")) {
            return;
        }
        Rol rolAdmin = rolRepository.findByCodigo("ADMIN").orElse(null);
        if (rolAdmin == null) {
            log.warn("No se encontró el rol ADMIN; no se crea el usuario inicial");
            return;
        }
        if (adminInitialPassword == null || adminInitialPassword.isBlank()) {
            log.warn("No hay contraseña inicial (ADMIN_INITIAL_PASSWORD) y no es el perfil dev; "
                    + "no se crea el admin");
            return;
        }
        Usuario admin = new Usuario("admin", passwordEncoder.encode(adminInitialPassword),
                "Administrador del sistema", rolAdmin);
        usuarioRepository.save(admin);
        log.info("Usuario inicial 'admin' creado con el rol ADMIN");
    }
}