package com.restaurante.auditoria;

import com.restaurante.AbstractIntegracionApi;
import com.restaurante.auditoria.domain.EventoAuditoria;
import com.restaurante.auditoria.infrastructure.AuditoriaRepository;
import com.restaurante.pagos.CuentaCobrada;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * El historial de auditoría (RF-52) debe reflejar lo que ocurrió, no lo que se
 * intentó. Con un listener síncrono la fila entraba en su propia transacción
 * (REQUIRES_NEW) y sobrevivía al rollback del negocio, así que un cobro
 * revertido aparecía como CUENTA_COBRADA: dinero que jamás se cobró, pero
 * respaldado por el historial. Por eso los eventos se escuchan en AFTER_COMMIT.
 */
class AuditoriaRollbackIntegrationTest extends AbstractIntegracionApi {

    @Autowired
    private ApplicationEventPublisher publisher;

    @Autowired
    private AuditoriaRepository auditoriaRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void unNegocioRevertidoNoDejaRastroEnLaAuditoria() {
        Long cuentaId = 9_000_000_000L + Math.abs(System.nanoTime() % 1_000_000L);

        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        assertThrows(IllegalStateException.class, () -> tx.executeWithoutResult(estado -> {
            publisher.publishEvent(cobro(cuentaId));
            throw new IllegalStateException("el cobro falla despues de publicar el evento");
        }));

        assertTrue(eventos(cuentaId).isEmpty(),
                "un cobro revertido no debe aparecer como cobrado en el historial");
    }

    @Test
    void unNegocioConfirmadoSiQuedaAuditado() {
        Long cuentaId = 9_000_000_000L + Math.abs(System.nanoTime() % 1_000_000L);

        new TransactionTemplate(transactionManager)
                .executeWithoutResult(estado -> publisher.publishEvent(cobro(cuentaId)));

        List<EventoAuditoria> eventos = eventos(cuentaId);
        assertEquals(1, eventos.size(), "el cobro confirmado debe quedar en el historial");
        assertEquals("CUENTA_COBRADA", eventos.get(0).getTipo());
    }

    private CuentaCobrada cobro(Long cuentaId) {
        return new CuentaCobrada(cuentaId, 1L, new BigDecimal("28.00"), 1L, "R-000999",
                List.of(new CuentaCobrada.PagoResumen(1L, "EFECTIVO", new BigDecimal("28.00"))));
    }

    private List<EventoAuditoria> eventos(Long entidadId) {
        return auditoriaRepository.findAll().stream()
                .filter(e -> e.getEntidadId().equals(String.valueOf(entidadId)))
                .toList();
    }
}