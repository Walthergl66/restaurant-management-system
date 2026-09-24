package com.restaurante.clientes.infrastructure;

import com.restaurante.clientes.PedidoClienteEventos;
import com.restaurante.clientes.PedidoClienteSPI;
import com.restaurante.clientes.application.ClientesService;
import com.restaurante.shared.outbox.EstadoOutbox;
import com.restaurante.shared.outbox.EventoOutbox;
import com.restaurante.shared.outbox.OutboxRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Consumidor del outbox {@code pedido-cliente-estado} (RF-43): difunde por STOMP
 * el estado actual al tópico del pedido. Reintentos "al menos una vez" sobre
 * los FALLIDO hasta un máximo de intentos; los que se agotan quedan marcados
 * para diagnóstico del agente de operaciones.
 */
@Component
public class DifusorEstadoPedidoCliente {

    static final int MAX_INTENTOS = 5;
    private static final int LOTE = 50;

    private final OutboxRepository outboxRepository;
    private final ClientesService clientesService;
    private final SimpMessagingTemplate messaging;

    public DifusorEstadoPedidoCliente(OutboxRepository outboxRepository,
                                      ClientesService clientesService,
                                      SimpMessagingTemplate messaging) {
        this.outboxRepository = outboxRepository;
        this.clientesService = clientesService;
        this.messaging = messaging;
    }

    @Scheduled(fixedDelayString = "${app.stomp.difusor-delay-ms:2000}",
            initialDelayString = "${app.stomp.difusor-initial-delay-ms:5000}")
    @Transactional
    public void difundirPendientes() {
        List<EventoOutbox> pendientes = outboxRepository.buscarPorTipoYEstados(
                PedidoClienteEventos.TIPO_ESTADO,
                List.of(EstadoOutbox.PENDIENTE, EstadoOutbox.FALLIDO),
                PageRequest.of(0, LOTE));
        for (EventoOutbox evento : pendientes) {
            if (EstadoOutbox.FALLIDO.equals(evento.getEstado())
                    && evento.getIntentos() >= MAX_INTENTOS) {
                continue;
            }
            difundir(evento);
        }
    }

    private void difundir(EventoOutbox evento) {
        try {
            PedidoClienteSPI spi = clientesService.estadoParaDifusion(evento.getPedidoCodigo());
            messaging.convertAndSend(PedidoClienteEventos.TOPIC_ESTADO + evento.getPedidoCodigo(), spi);
            evento.marcarEnviado();
        } catch (Exception e) {
            evento.marcarFallido(abreviar(e));
        }
    }

    private String abreviar(Exception e) {
        String mensaje = e.getMessage();
        if (mensaje == null || mensaje.isBlank()) {
            return e.getClass().getSimpleName();
        }
        return mensaje.length() > 200 ? mensaje.substring(0, 200) : mensaje;
    }
}