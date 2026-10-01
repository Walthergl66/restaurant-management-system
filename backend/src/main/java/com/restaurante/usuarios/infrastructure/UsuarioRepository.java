package com.restaurante.usuarios.infrastructure;

import com.restaurante.usuarios.domain.Usuario;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByUsername(String username);

    /**
     * Usuario con la fila bloqueada en escritura. Lo usa el módulo de
     * clientes para serializar la creación on-the-fly del cliente (RF-42):
     * sin el lock, dos peticiones concurrentes del mismo usuario pasan a la
     * vez el "no existe cliente" y ambas insertan, y una revienta con
     * violación de la clave única {@code usuario_id}.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from Usuario u where u.username = :username")
    Optional<Usuario> findBloqueadoPorUsername(@Param("username") String username);

    Optional<Usuario> findByUsernameAndActivoTrue(String username);

    boolean existsByUsername(String username);

    @Query("select u from Usuario u left join fetch u.rol where u.id = :id")
    Optional<Usuario> findByIdWithRol(@Param("id") Long id);

    /** A-04: versión de sesión vigente del usuario activo, para compararla
     *  con la embebida en el JWT sin cargar la entidad completa. */
    @Query("select u.sesionVersion from Usuario u where u.username = :username and u.activo = true")
    Optional<Long> findSesionVersionActivo(@Param("username") String username);
}