package com.restaurante.comandas.application;

import com.restaurante.clientes.Clientes;
import com.restaurante.comandas.domain.Comanda;
import com.restaurante.comandas.domain.ComandaEstado;
import com.restaurante.comandas.domain.TipoComanda;
import com.restaurante.comandas.infrastructure.ComandaRepository;
import com.restaurante.comandas.web.dto.ComandaResponse;
import com.restaurante.comandas.web.dto.ImpresionResponse;
import com.restaurante.pedidos.Pedidos;
import com.restaurante.shared.domain.exception.NotFoundException;
import com.restaurante.shared.outbox.EstadoOutbox;
import com.restaurante.shared.outbox.EventoOutbox;
import com.restaurante.shared.outbox.OutboxRepository;
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
    private final Clientes clientes;

    public ComandaService(ComandaRepository comandaRepository, OutboxRepository outboxRepository,
                          Pedidos pedidos, Clientes clientes) {
        this.comandaRepository = comandaRepository;
        this.outboxRepository = outboxRepository;
        this.pedidos = pedidos;
        this.clientes = clientes;
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
        avanzarSiTodoElPedidoAvanza(comanda.getPedidoCodigo(), false);
        return ComandaResponse.from(comanda);
    }

    public ComandaResponse marcarListo(Long id) {
        Comanda comanda = cargar(id);
        comanda.marcarListo();
        avanzarSiTodoElPedidoAvanza(comanda.getPedidoCodigo(), true);
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
        List<EventoOutbox> pendientes = outboxRepository.buscarPorTipoYEstados(
                GeneradorComandas.TIPO_OUTBOX,
                List.of(EstadoOutbox.PENDIENTE),
                PageRequest.of(0, TAMANO_LOTE_IMPRESION));
        return pendientes.stream().map(ImpresionResponse::from).toList();
    }

    /**
     * El agente confirma que imprimió la orden; deja de entregarse (al menos
     * una vez: si no confirma, vuelve a aparecer en pendientes).
     */
    public void marcarEnviadaImpresion(Long id) {
        EventoOutbox evento = outboxRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Orden de impresión " + id + " no encontrada"));
        evento.marcarEnviado();
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

    /**
     * Un pedido genera una comanda por área, así que el estado del pedido no
     * puede depender de una sola: con el paso de una comanda, el pedido quedaba
     * "en preparación" o "listo" mientras otra cocina no había empezado, y el
     * mesero veía un pedido que aún no estaba. El pedido avanza cuando todas
     * sus áreas llegaron al estado, y solo se consideran las comandas de
     * orden: las de cancelación son instrucciones aparte y su propio ciclo.
     */
    private void avanzarSiTodoElPedidoAvanza(String codigo, boolean listo) {
        List<Comanda> areas = comandaRepository.findPorPedido(codigo).stream()
                .filter(c -> c.getTipo() == TipoComanda.ORDEN)
                .toList();
        boolean todasListas = areas.stream().allMatch(c -> c.getEstado() == ComandaEstado.LISTO);
        boolean todasPreparando = areas.stream()
                .allMatch(c -> c.getEstado() == ComandaEstado.EN_PREPARACION
                        || c.getEstado() == ComandaEstado.LISTO);
        if (listo) {
            if (todasListas) {
                avanzar(codigo, true);
            }
        } else if (todasPreparando) {
            avanzar(codigo, false);
        }
    }

    /**
     * Avanza el pedido presencial (RF-14/RF-15); si el código pertenece a un
     * pedido de la app del cliente, avanza su estado vía el SPI de clientes
     * (RF-44). No se usa try/catch sobre el SPI de pedidos porque su
     * NotFound marcaría la transacción rollback-only.
     */
    private void avanzar(String codigo, boolean listo) {
        if (pedidos.existe(codigo)) {
            if (listo) {
                pedidos.marcarListo(codigo);
            } else {
                pedidos.marcarEnPreparacion(codigo);
            }
        } else if (listo) {
            clientes.marcarListo(codigo);
        } else {
            clientes.marcarEnPreparacion(codigo);
        }
    }
}