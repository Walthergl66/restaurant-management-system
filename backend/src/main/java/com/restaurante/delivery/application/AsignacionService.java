package com.restaurante.delivery.application;

import com.restaurante.auditoria.Auditoria;
import com.restaurante.clientes.Clientes;
import com.restaurante.clientes.PedidoDomicilioSPI;
import com.restaurante.delivery.domain.Asignacion;
import com.restaurante.delivery.domain.EstadoAsignacion;
import com.restaurante.delivery.domain.EstadoRepartidor;
import com.restaurante.delivery.domain.Repartidor;
import com.restaurante.delivery.infrastructure.AsignacionRepository;
import com.restaurante.delivery.infrastructure.RepartidorRepository;
import com.restaurante.shared.domain.exception.BusinessRuleException;
import com.restaurante.shared.domain.exception.ConflictException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Asignación de pedidos a domicilio a repartidores (lo opera el ADMIN). Maneja
 * el estado LOGÍSTICO en {@code delivery.asignaciones} sin tocar el estado de
 * preparación del pedido en comercial: 'listo para reparto' lo define el
 * módulo clientes (DOMICILIO + LISTO) y aquí solo se consulta por su SPI.
 *
 * <p>Un pedido solo puede tener una asignación ACTIVA a la vez; lo garantiza
 * el índice único parcial de la tabla (estado NOT IN REASIGNADA/FALLIDA) junto
 * a {@code @Version}. Reasignar pasa la asignación vigente a REASIGNADA y crea
 * la nueva en la MISMA transacción para no chocar con ese índice.
 */
@Service
@Transactional
public class AsignacionService {

    private static final String CATEGORIA_AUDITORIA = "DELIVERY";

    private final AsignacionRepository asignaciones;
    private final RepartidorRepository repartidores;
    private final Clientes clientes;
    private final Auditoria auditoria;
    private final ObjectMapper objectMapper;

    public AsignacionService(AsignacionRepository asignaciones, RepartidorRepository repartidores,
                             Clientes clientes, Auditoria auditoria, ObjectMapper objectMapper) {
        this.asignaciones = asignaciones;
        this.repartidores = repartidores;
        this.clientes = clientes;
        this.auditoria = auditoria;
        this.objectMapper = objectMapper;
    }

    /**
     * Pedidos a DOMICILIO en estado LISTO que aún no tienen una asignación
     * activa: lo que el administrador puede repartir ahora.
     */
    @Transactional(readOnly = true)
    public List<PedidoListoParaAsignar> listarPedidosListosParaAsignar() {
        return clientes.pedidosDomicilioListos().stream()
                .filter(p -> asignaciones.findActivaByPedidoClienteId(p.pedidoClienteId()).isEmpty())
                .map(PedidoListoParaAsignar::de)
                .toList();
    }

    /**
     * Asigna un pedido a domicilio a un repartidor habilitado. 422 si el
     * repartidor no está habilitado, si el pedido no es DOMICILIO/LISTO, o si
     * ya tiene una asignación activa; 409 si dos asignaciones compiten por el
     * mismo pedido (el índice único parcial deja ganar a una sola).
     */
    public AsignacionResumen asignar(Long pedidoClienteId, Long repartidorId, String actorAdmin) {
        validarRepartidorHabilitado(repartidorId);
        validarPedidoListoDomicilio(pedidoClienteId);
        if (asignaciones.findActivaByPedidoClienteId(pedidoClienteId).isPresent()) {
            throw new BusinessRuleException("El pedido ya tiene una asignación activa");
        }
        Asignacion asignacion = crearAsignacion(pedidoClienteId, repartidorId, actorAdmin);
        auditarAsignacion("ASIGNACION_CREADA", asignacion);
        return resumen(asignacion);
    }

    /**
     * Asigna un lote de pedidos al mismo repartidor (multientrega del lado
     * admin). ATÓMICO: valida TODOS los pedidos y el repartidor antes de crear
     * ninguna asignación; si alguno falla la validación, no se crea ninguna.
     */
    public List<AsignacionResumen> asignarVarios(List<Long> pedidoClienteIds, Long repartidorId,
                                                 String actorAdmin) {
        if (pedidoClienteIds == null || pedidoClienteIds.isEmpty()) {
            throw new BusinessRuleException("Debe indicar al menos un pedido para asignar");
        }
        validarRepartidorHabilitado(repartidorId);
        List<Long> distintos = pedidoClienteIds.stream().distinct().toList();
        if (distintos.size() != pedidoClienteIds.size()) {
            throw new BusinessRuleException("La lista de pedidos no puede tener duplicados");
        }
        // Validar todos primero: así un pedido inválido no deja asignaciones a medias.
        for (Long pedidoClienteId : distintos) {
            validarPedidoListoDomicilio(pedidoClienteId);
            if (asignaciones.findActivaByPedidoClienteId(pedidoClienteId).isPresent()) {
                throw new BusinessRuleException(
                        "El pedido " + pedidoClienteId + " ya tiene una asignación activa");
            }
        }
        List<AsignacionResumen> creadas = new ArrayList<>();
        for (Long pedidoClienteId : distintos) {
            Asignacion asignacion = crearAsignacion(pedidoClienteId, repartidorId, actorAdmin);
            auditarAsignacion("ASIGNACION_CREADA", asignacion);
            creadas.add(resumen(asignacion));
        }
        return creadas;
    }

    /**
     * Reasigna un pedido a otro repartidor: en la MISMA transacción marca
     * REASIGNADA la asignación vigente y crea una nueva ASIGNADA. 422 si no hay
     * asignación activa, si ya fue entregada, o si salió de ruta (solo una
     * asignación ASIGNADA puede liberarse).
     */
    public AsignacionResumen reasignar(Long pedidoClienteId, Long nuevoRepartidorId, String actorAdmin) {
        validarRepartidorHabilitado(nuevoRepartidorId);
        Asignacion vigente = asignaciones.findActivaByPedidoClienteId(pedidoClienteId)
                .orElseThrow(() -> new BusinessRuleException(
                        "El pedido no tiene una asignación activa para reasignar"));
        if (vigente.getEstado() == EstadoAsignacion.ENTREGADA) {
            throw new BusinessRuleException("El pedido ya fue entregado: no puede reasignarse");
        }
        if (vigente.getEstado() != EstadoAsignacion.ASIGNADA) {
            throw new BusinessRuleException(
                    "Solo una asignación aún no despachada puede reasignarse (estado: "
                            + vigente.getEstado() + ")");
        }
        vigente.reasignar();
        asignaciones.saveAndFlush(vigente);
        Asignacion nueva = crearAsignacion(pedidoClienteId, nuevoRepartidorId, actorAdmin);
        auditoria.registrar("ASIGNACION_REASIGNADA", "ASIGNACION",
                String.valueOf(nueva.getId()),
                json(Map.of(
                        "pedidoClienteId", String.valueOf(pedidoClienteId),
                        "asignacionAnterior", String.valueOf(vigente.getId()),
                        "repartidorAnterior", String.valueOf(vigente.getRepartidorId()),
                        "repartidorNuevo", String.valueOf(nuevoRepartidorId))),
                CATEGORIA_AUDITORIA);
        return resumen(nueva);
    }

    /**
     * Estados de reparto para el panel admin, opcionalmente filtrados por
     * estado logístico y/o repartidor.
     */
    @Transactional(readOnly = true)
    public List<AsignacionResumen> listarAsignaciones(EstadoAsignacion estado, Long repartidorId) {
        List<Asignacion> encontradas;
        if (estado != null && repartidorId != null) {
            encontradas = asignaciones.findByEstadoAndRepartidorIdOrderByIdDesc(estado, repartidorId);
        } else if (estado != null) {
            encontradas = asignaciones.findByEstadoOrderByIdDesc(estado);
        } else if (repartidorId != null) {
            encontradas = asignaciones.findByRepartidorIdOrderByIdDesc(repartidorId);
        } else {
            encontradas = asignaciones.findAllByOrderByIdDesc();
        }
        return encontradas.stream().map(this::resumen).toList();
    }

    // ---- internos ----

    private Asignacion crearAsignacion(Long pedidoClienteId, Long repartidorId, String actorAdmin) {
        try {
            return asignaciones.saveAndFlush(
                    new Asignacion(pedidoClienteId, repartidorId, actorAdmin));
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            // Choque con el índice único parcial: otra transacción ganó la carrera.
            throw new ConflictException("El pedido ya tiene una asignación activa");
        }
    }

    private void validarRepartidorHabilitado(Long repartidorId) {
        Repartidor repartidor = repartidores.findById(repartidorId)
                .orElseThrow(() -> new BusinessRuleException("El repartidor no existe"));
        if (repartidor.getEstado() != EstadoRepartidor.HABILITADO) {
            throw new BusinessRuleException("El repartidor no está habilitado para operar");
        }
    }

    private void validarPedidoListoDomicilio(Long pedidoClienteId) {
        PedidoDomicilioSPI pedido = clientes.datosEntrega(pedidoClienteId)
                .orElseThrow(() -> new BusinessRuleException("El pedido no existe"));
        if (!"DOMICILIO".equals(pedido.metodoEntrega())) {
            throw new BusinessRuleException("El pedido no es para reparto a domicilio");
        }
        if (!"LISTO".equals(pedido.estado())) {
            throw new BusinessRuleException(
                    "El pedido no está listo para reparto (estado: " + pedido.estado() + ")");
        }
    }

    private void auditarAsignacion(String tipo, Asignacion asignacion) {
        Map<String, String> detalle = new LinkedHashMap<>();
        detalle.put("pedidoClienteId", String.valueOf(asignacion.getPedidoClienteId()));
        detalle.put("repartidorId", String.valueOf(asignacion.getRepartidorId()));
        auditoria.registrar(tipo, "ASIGNACION", String.valueOf(asignacion.getId()),
                json(detalle), CATEGORIA_AUDITORIA);
    }

    private AsignacionResumen resumen(Asignacion asignacion) {
        PedidoDomicilioSPI datos = clientes.datosEntrega(asignacion.getPedidoClienteId()).orElse(null);
        return AsignacionResumen.de(asignacion, datos);
    }

    private String json(Map<String, String> detalle) {
        try {
            return objectMapper.writeValueAsString(detalle);
        } catch (Exception e) {
            return "{\"detalle\":\"no serializable\"}";
        }
    }
}
