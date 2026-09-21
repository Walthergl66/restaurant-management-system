package com.restaurante.comandas.application;

import com.restaurante.anulaciones.AnulacionAprobada;
import com.restaurante.comandas.domain.Comanda;
import com.restaurante.comandas.domain.ComandaLinea;
import com.restaurante.comandas.domain.EventoOutbox;
import com.restaurante.comandas.domain.TipoComanda;
import com.restaurante.comandas.infrastructure.ComandaRepository;
import com.restaurante.comandas.infrastructure.OutboxRepository;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reacciona a la aprobación de una anulación, en la misma transacción,
 * generando la comanda de cancelación del área y su orden de impresión
 * (RF-20 a RF-23). Idempotente: el índice único sobre anulacion_id impide
 * duplicados aunque el evento se procese dos veces.
 */
@Component
public class GeneradorComandaCancelacion {

    public static final String EVENTO_CANCELACION_IMPRESION = "comanda.cancelacion.impresion";

    private final ComandaRepository comandaRepository;
    private final OutboxRepository outboxRepository;

    public GeneradorComandaCancelacion(ComandaRepository comandaRepository, OutboxRepository outboxRepository) {
        this.comandaRepository = comandaRepository;
        this.outboxRepository = outboxRepository;
    }

    @EventListener
    @Transactional
    public void alAprobar(AnulacionAprobada evento) {
        if (evento.areaId() == null) {
            return;
        }
        if (comandaRepository.existsByAnulacionId(evento.anulacionId())) {
            return;
        }
        int numero = comandaRepository.siguienteNumeroComanda().intValue();
        Comanda comanda = new Comanda(
                evento.pedidoCodigo(),
                numero,
                evento.areaId(),
                evento.areaNombre(),
                TipoComanda.CANCELACION,
                evento.anulacionId());
        String observacion = evento.motivo() == null || evento.motivo().isBlank()
                ? "ANULACIÓN"
                : "ANULACIÓN: " + evento.motivo();
        comanda.agregarLinea(new ComandaLinea(
                evento.productoId(),
                evento.nombreProducto(),
                evento.cantidad(),
                null,
                null,
                observacion,
                1));
        comandaRepository.save(comanda);

        outboxRepository.save(new EventoOutbox(
                GeneradorComandas.TIPO_OUTBOX,
                String.valueOf(numero),
                evento.pedidoCodigo(),
                numero,
                evento.areaId(),
                evento.areaNombre(),
                EVENTO_CANCELACION_IMPRESION));
    }
}