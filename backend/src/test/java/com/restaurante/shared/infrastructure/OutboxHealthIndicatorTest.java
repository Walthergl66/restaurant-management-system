package com.restaurante.shared.infrastructure;

import com.restaurante.shared.outbox.EstadoOutbox;
import com.restaurante.shared.outbox.OutboxRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.health.contributor.Status;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Plan mejoras 4: el indicador marca DOWN cuando el outbox acumula
 *  pendientes viejos o fallidos por encima del umbral. */
class OutboxHealthIndicatorTest {

    private OutboxHealthIndicator con(long pendientes, long fallidos) {
        OutboxRepository repo = mock(OutboxRepository.class);
        when(repo.contarPorEstadoAntesDe(eq(EstadoOutbox.PENDIENTE), any())).thenReturn(pendientes);
        when(repo.contarPorEstadoAntesDe(eq(EstadoOutbox.FALLIDO), any())).thenReturn(fallidos);
        return new OutboxHealthIndicator(repo, 5, 50, 10);
    }

    @Test
    void conAcumulacionNormalReportaUp() {
        assertEquals(Status.UP, con(3, 1).health().getStatus());
    }

    @Test
    void muchosPendientesViejosReportaDown() {
        assertEquals(Status.DOWN, con(51, 0).health().getStatus());
    }

    @Test
    void muchosFallidosReportaDown() {
        assertEquals(Status.DOWN, con(0, 11).health().getStatus());
    }

    @Test
    void consultaAmbosEstadosConCorte() {
        OutboxRepository repo = mock(OutboxRepository.class);
        when(repo.contarPorEstadoAntesDe(eq(EstadoOutbox.PENDIENTE), any())).thenReturn(0L);
        when(repo.contarPorEstadoAntesDe(eq(EstadoOutbox.FALLIDO), any())).thenReturn(0L);
        new OutboxHealthIndicator(repo, 5, 50, 10).health();
        verify(repo).contarPorEstadoAntesDe(eq(EstadoOutbox.PENDIENTE), any());
        verify(repo).contarPorEstadoAntesDe(eq(EstadoOutbox.FALLIDO), any());
    }
}