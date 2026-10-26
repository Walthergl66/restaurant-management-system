package com.restaurante.archivos.application;

import com.restaurante.archivos.infrastructure.*;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.Instant;
import java.time.Duration;

@Component
public class LimpiezaArchivos {
    private final ArchivoRepository repository;
    private final AlmacenArchivos almacen;
    private final ArchivoProperties config;
    private final TransactionTemplate transaccion;
    public LimpiezaArchivos(ArchivoRepository repository, AlmacenArchivos almacen, ArchivoProperties config, PlatformTransactionManager manager) {
        this.repository=repository; this.almacen=almacen; this.config=config; this.transaccion=new TransactionTemplate(manager);
    }
    @Scheduled(fixedDelayString="${app.archivos.limpieza-intervalo-ms:600000}")
    public void limpiar() {
        if (!config.isHabilitado()) return;
        Instant corte=Instant.now().minus(Duration.ofDays(1));
        for (var id : repository.candidatos(corte, PageRequest.of(0, 50))) {
            try {
                transaccion.executeWithoutResult(status -> repository.bloquear(id).ifPresent(a -> {
                    if (!"VINCULADO".equals(a.getEstado())) {
                        almacen.eliminar(a.getClave()); repository.delete(a);
                    }
                }));
            } catch (RuntimeException ex) { LoggerFactory.getLogger(getClass()).warn("No se pudo limpiar archivo {}; se reintentará", id, ex); }
        }
    }
}
