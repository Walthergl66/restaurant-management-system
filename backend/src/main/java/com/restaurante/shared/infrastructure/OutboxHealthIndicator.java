package com.restaurante.shared.infrastructure;

import com.restaurante.shared.outbox.EstadoOutbox;
import com.restaurante.shared.outbox.OutboxRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

/**
 * Salud del outbox (plan mejoras 4): reporta DOWN si hay demasiadas órdenes
 * PENDIENTES viejas o FALLIDAS (posible agotamiento de reintentos). Umbrales
 * configurables por {@code app.monitoreo.outbox.*}; en prod los detalles no se
 * exponen (health.show-details: never), solo el estado global UP/DOWN.
 */
@Component
public class OutboxHealthIndicator implements HealthIndicator {

    private final OutboxRepository outboxRepository;
    private final Duration antiguedad;
    private final long maxPendientes;
    private final long maxFallidos;

    public OutboxHealthIndicator(OutboxRepository outboxRepository,
                                 @Value("${app.monitoreo.outbox.antiguedad-min:5}") long antiguedadMin,
                                 @Value("${app.monitoreo.outbox.max-pendientes:50}") long maxPendientes,
                                 @Value("${app.monitoreo.outbox.max-fallidos:10}") long maxFallidos) {
        this.outboxRepository = outboxRepository;
        this.antiguedad = Duration.ofMinutes(antiguedadMin);
        this.maxPendientes = maxPendientes;
        this.maxFallidos = maxFallidos;
    }

    @Override
    public Health health() {
        Instant corte = Instant.now().minus(antiguedad);
        long pendientes = outboxRepository.contarPorEstadoAntesDe(EstadoOutbox.PENDIENTE, corte);
        long fallidos = outboxRepository.contarPorEstadoAntesDe(EstadoOutbox.FALLIDO, corte);
        Health.Builder builder = pendientes > maxPendientes || fallidos > maxFallidos
                ? Health.down()
                : Health.up();
        return builder
                .withDetail("pendientesViejos", pendientes)
                .withDetail("fallidos", fallidos)
                .build();
    }
}