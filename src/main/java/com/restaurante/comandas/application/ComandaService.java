package com.restaurante.comandas.application;

import com.restaurante.comandas.domain.Comanda;
import com.restaurante.comandas.domain.ComandaEstado;
import com.restaurante.comandas.domain.EstadoOutbox;
import com.restaurante.comandas.domain.EventoOutbox;
import com.restaurante.comandas.infrastructure.ComandaRepository;
import com.restaurante.comandas.infrastructure.OutboxRepository;
import com.restaurante.comandas.web.dto.ComandaResponse;
import com.restaurante.comandas.web.dto.ImpresionResponse;
import com.restaurante.pedidos.Pedidos;
import com.restaurante.shared.domain.exception.NotFoundException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Comandas por área: listado, estados de cocina (RF-11 a RF-14), reimpresión
 * y consumo de órdenes de impresión por el agente local.
 */
@Service
@Transactional
public class ComandaService {

    private static final int TAMANO_LOTE_IMPRESION = 50;

    private final ComandaRepository comandaRepository;
    private final OutboxRepository outboxRepository;
    private final Pedidos pedidos;

    public ComandaService(ComandaRepository comandaRepository, OutboxRepository outboxRepository, Pedidos pedidos) {
        this.comandaRepository = comandaRepository;
        this.outboxRepository = outboxRepository;
        this.pedidos = pedidos;
    }

    @Transactional(readOnly = true)
    public List<ComandaResponse> listar(Long areaId, ComandaEstado estado) {
        return comandaRepository.buscarConLineas(areaId, estado).stream()
                .map(ComandaResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ComandaResponse obtener(Long id) {
        return ComandaResponse.from(cargar(id));
    }

    public ComandaResponse marcarEnPreparacion(Long id) {
        Comanda comanda = cargar(id);
        comanda.marcarEnPreparacion();
        pedidos.marcarEnPreparacion(comanda.getPedidoCodigo());
        return ComandaResponse.from(comanda);
    }

    public ComandaResponse marcarListo(Long id) {
        Comanda comanda = cargar(id);
        comanda.marcarListo();
        pedidos.marcarListo(comanda.getPedidoCodigo());
        return ComandaResponse.from(comanda);
    }

    public ComandaResponse reimprimir(Long id) {
        Comanda comanda = cargar(id);
        outboxRepository.save(new EventoOutbox(
                GeneradorComandas.TIPO_OUTBOX,
                String.valueOf(comanda.getNumeroComanda()),
                comanda.getPedidoCodigo(),
                comanda.getNumeroComanda(),
                comanda.getAreaId(),
                comanda.getAreaNombre(),
                GeneradorComandas.EVENTO_IMPRESION));
        return ComandaResponse.from(comanda);
    }

    @Transactional(readOnly = true)
    public List<ImpresionResponse> imprimirPendientes() {
        List<EventoOutbox> pendientes =
                outboxRepository.buscarPorEstado(EstadoOutbox.PENDIENTE, PageRequest.of(0, TAMANO_LOTE_IMPRESION));
        return pendientes.stream().map(ImpresionResponse::from).toList();
    }

    /**
     * Marca la orden como fallida para reintentarla; el agente reporta el error.
     */
    public void marcarErrorImpresion(Long id, String motivo) {
        EventoOutbox evento = outboxRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Orden de impresión " + id + " no encontrada"));
        evento.marcarFallido(motivo);
    }

    private Comanda cargar(Long id) {
        return comandaRepository.findByIdConLineas(id)
                .orElseThrow(() -> new NotFoundException("Comanda " + id + " no encontrada"));
    }
}