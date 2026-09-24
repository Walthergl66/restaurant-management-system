package com.restaurante.usuarios.infrastructure;

import com.restaurante.usuarios.domain.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByUsername(String username);

    Optional<Usuario> findByUsernameAndActivoTrue(String username);

    boolean existsByUsername(String username);

    @Query("select u from Usuario u left join fetch u.rol where u.id = :id")
    Optional<Usuario> findByIdWithRol(@Param("id") Long id);

    /** A-04: versión de sesión vigente del usuario activo, para compararla
     *  con la embebida en el JWT sin cargar la entidad completa. */
    @Query("select u.sesionVersion from Usuario u where u.username = :username and u.activo = true")
    Optional<Long> findSesionVersionActivo(@Param("username") String username);
}