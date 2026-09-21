package com.restaurante.comandas.application;

import com.restaurante.comandas.domain.Comanda;
import com.restaurante.comandas.domain.ComandaLinea;
import com.restaurante.comandas.domain.EventoOutbox;
import com.restaurante.comandas.infrastructure.ComandaRepository;
import com.restaurante.comandas.infrastructure.OutboxRepository;
import com.restaurante.pedidos.PedidoConfirmado;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Reacciona a la confirmación de un pedido, en la mísma transacción, generando
 * una comanda por cada área con productos y una orden de impresión en el outbox.
 * Idempotente: la UNIQUE (pedido_codigo, area_id) protege de duplicados aunque
 * el evento se procese dos veces (RNF-17).
 */
@Component
public class GeneradorComandas {

    public static final String TIPO_OUTBOX = "comanda";
    public static final String EVENTO_IMPRESION = "comanda.impresion";

    private final ComandaRepository comandaRepository;
    private final OutboxRepository outboxRepository;

    public GeneradorComandas(ComandaRepository comandaRepository, OutboxRepository outboxRepository) {
        this.comandaRepository = comandaRepository;
        this.outboxRepository = outboxRepository;
    }

    @EventListener
    @Transactional
    public void alConfirmar(PedidoConfirmado evento) {
        Map<Long, List<PedidoConfirmado.LineaConfirmada>> porArea = evento.lineas().stream()
                .filter(linea -> linea.areaId() != null)
                .collect(Collectors.groupingBy(PedidoConfirmado.LineaConfirmada::areaId, LinkedHashMap::new, Collectors.toList()));

        porArea.forEach((areaId, lineas) -> {
            String areaNombre = lineas.get(0).areaNombre();
            if (comandaRepository.existsByPedidoCodigoAndAreaId(evento.pedidoCodigo(), areaId)) {
                return;
            }
            int numero = comandaRepository.siguienteNumeroComanda().intValue();
            Comanda comanda = new Comanda(evento.pedidoCodigo(), numero, areaId, areaNombre);
            agregarLineas(comanda, lineas);
            comandaRepository.save(comanda);

            outboxRepository.save(new EventoOutbox(
                    TIPO_OUTBOX,
                    String.valueOf(numero),
                    evento.pedidoCodigo(),
                    numero,
                    areaId,
                    areaNombre,
                    EVENTO_IMPRESION));
        });
    }

    /**
     * Agrupa las líneas repetidas del mismo producto+extras+ingredientes
     * sumando cantidades y concatenando observaciones.
     */
    private void agregarLineas(Comanda comanda, List<PedidoConfirmado.LineaConfirmada> lineas) {
        Map<String, LineaAcumulada> agrupadas = new LinkedHashMap<>();
        for (PedidoConfirmado.LineaConfirmada l : lineas) {
            String clave = l.productoId()
                    + "|" + normalizar(l.extras())
                    + "|" + normalizar(l.ingredientesRemovidos());
            agrupadas.computeIfAbsent(clave, k -> new LineaAcumulada(
                            l.productoId(), l.nombreProducto(),
                            normalizar(l.extras()), normalizar(l.ingredientesRemovidos())))
                    .agregar(l);
        }
        int orden = 1;
        for (LineaAcumulada acumulada : agrupadas.values()) {
            comanda.agregarLinea(new ComandaLinea(
                    acumulada.productoId(),
                    acumulada.nombreProducto(),
                    acumulada.cantidad(),
                    acumulada.extras().isEmpty() ? null : acumulada.extras(),
                    acumulada.ingredientes().isEmpty() ? null : acumulada.ingredientes(),
                    String.join("; ", acumulada.observaciones()),
                    orden++));
        }
    }

    private String normalizar(List<String> valores) {
        return valores == null
                ? ""
                : valores.stream().filter(v -> v != null && !v.isBlank()).sorted().collect(Collectors.joining(","));
    }

    private static final class LineaAcumulada {
        private final Long productoId;
        private final String nombreProducto;
        private final String extras;
        private final String ingredientes;
        private int cantidad;
        private final List<String> observaciones = new ArrayList<>();

        private LineaAcumulada(Long productoId, String nombreProducto, String extras, String ingredientes) {
            this.productoId = productoId;
            this.nombreProducto = nombreProducto;
            this.extras = extras;
            this.ingredientes = ingredientes;
        }

        private void agregar(PedidoConfirmado.LineaConfirmada linea) {
            this.cantidad += linea.cantidad();
            if (linea.observaciones() != null && !linea.observaciones().isBlank()) {
                this.observaciones.add(linea.observaciones());
            }
        }
    }
}