package com.restaurante.archivos.infrastructure;

import com.restaurante.archivos.domain.Archivo;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import java.time.Instant;
import java.util.*;

public interface ArchivoRepository extends JpaRepository<Archivo, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Archivo a where a.id = :id")
    Optional<Archivo> bloquear(UUID id);

    @Query("select a.id from Archivo a where a.estado <> 'VINCULADO' and a.modificadoEn < :corte order by a.modificadoEn")
    List<UUID> candidatos(Instant corte, org.springframework.data.domain.Pageable pageable);
}
