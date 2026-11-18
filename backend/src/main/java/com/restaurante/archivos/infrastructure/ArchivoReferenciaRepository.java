package com.restaurante.archivos.infrastructure;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/** Consulta metadatos de archivos vinculados por la versión anterior del catálogo. */
@Repository
public class ArchivoReferenciaRepository {

    private final JdbcTemplate jdbcTemplate;

    public ArchivoReferenciaRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<String> buscarClaveVinculada(UUID id) {
        return jdbcTemplate.query(
                "SELECT clave FROM archivos.archivos WHERE id = ? AND estado = 'VINCULADO'",
                resultSet -> resultSet.next()
                        ? Optional.of(resultSet.getString("clave"))
                        : Optional.empty(),
                id);
    }
}
