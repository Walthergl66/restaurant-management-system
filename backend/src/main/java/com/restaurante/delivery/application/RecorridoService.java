package com.restaurante.delivery.application;

import com.restaurante.auditoria.Auditoria;
import com.restaurante.clientes.Clientes;
import com.restaurante.clientes.PedidoDomicilioSPI;
import com.restaurante.delivery.domain.Asignacion;
import com.restaurante.delivery.domain.Coordenada;
import com.restaurante.delivery.domain.CriterioOptimizacion;
import com.restaurante.delivery.domain.EstadoAsignacion;
import com.restaurante.delivery.domain.EstadoParada;
import com.restaurante.delivery.domain.EstadoRecorrido;
import com.restaurante.delivery.domain.FaseConfirmacion;
import com.restaurante.delivery.domain.Incidencia;
import com.restaurante.delivery.domain.Parada;
import com.restaurante.delivery.domain.Recorrido;
import com.restaurante.delivery.domain.Repartidor;
import com.restaurante.delivery.domain.ResultadoRuta;
import com.restaurante.delivery.domain.RutasNoDisponiblesException;
import com.restaurante.delivery.domain.TipoRecorrido;
import com.restaurante.delivery.infrastructure.AsignacionRepository;
import com.restaurante.delivery.infrastructure.IncidenciaRepository;
import com.restaurante.delivery.infrastructure.ParadaRepository;
import com.restaurante.delivery.infrastructure.RecorridoRepository;
import com.restaurante.delivery.infrastructure.RutasService;
import com.restaurante.shared.domain.exception.BusinessRuleException;
import com.restaurante.shared.domain.exception.NotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Flujo OPERATIVO del repartidor (FASE 5): rutas, optimización del orden de
 * paradas y recorridos. Siempre a través de NUESTRO backend; el móvil nunca
 * llama a ORS. Todo lo que opera un repartidor exige dos guardas:
 *
 * <ol>
 *   <li>El usuario autenticado debe ser un repartidor HABILITADO
 *       ({@link RepartidorService#repartidorHabilitadoDelUsuarioActual()}).</li>
 *   <li>PROPIEDAD: el recurso (asignación/parada/recorrido) debe ser de ESE
 *       repartidor. Un repartidor no puede operar lo de otro: se verifica en
 *       backend (no en la UI) y responde 403.</li>
 * </ol>
 *
 * <p>Los estados que maneja son LOGÍSTICOS (asignación/recorrido/parada) y
 * NUNCA tocan el estado de preparación del pedido en comercial: iniciar un
 * recorrido o entregar una parada no modifica {@code comercial.pedidos_clientes}.
 *
 * <p>Origen de las rutas: la ubicación operativa del restaurante, PERSISTIDA y
 * editable por el administrador ({@link ConfigRestauranteService#origenParaRutas()}).
 * Ya no depende de env vars ({@code app.delivery.origen}) ni de coordenadas
 * hardcodeadas; si no está configurada, el servicio falla fail-closed (503).
 *
 * <p>Criterio de optimización por defecto del sistema: {@link CriterioOptimizacion#DURACION}
 * (ruta más rápida), salvo que el request indique {@code DISTANCIA}.
 */
@Service
@Transactional
public class RecorridoService {

    private static final String CATEGORIA_AUDITORIA = "DELIVERY";

    /** Criterio del sistema cuando el request no indica otro: la ruta más rápida. */
    static final CriterioOptimizacion CRITERIO_POR_DEFECTO = CriterioOptimizacion.DURACION;

    private final RecorridoRepository recorridos;
    private final ParadaRepository paradas;
    private final AsignacionRepository asignaciones;
    private final IncidenciaRepository incidencias;
    private final RepartidorService repartidorService;
    private final RutasService rutas;
    private final ConfigRestauranteService configRestaurante;
    private final Clientes clientes;
    private final Auditoria auditoria;
    private final ObjectMapper objectMapper;

    public RecorridoService(RecorridoRepository recorridos, ParadaRepository paradas,
                            AsignacionRepository asignaciones, IncidenciaRepository incidencias,
                            RepartidorService repartidorService, RutasService rutas,
                            ConfigRestauranteService configRestaurante, Clientes clientes,
                            Auditoria auditoria, ObjectMapper objectMapper) {
        this.recorridos = recorridos;
        this.paradas = paradas;
        this.asignaciones = asignaciones;
        this.incidencias = incidencias;
        this.repartidorService = repartidorService;
        this.rutas = rutas;
        this.configRestaurante = configRestaurante;
        this.clientes = clientes;
        this.auditoria = auditoria;
        this.objectMapper = objectMapper;
    }

    /**
     * Lo que el repartidor autenticado (HABILITADO) ve de su trabajo activo:
     * sus asignaciones vigentes y sus recorridos no finalizados, con el estado
     * logístico y los datos de entrega. Solo lo SUYO.
     */
    @Transactional(readOnly = true)
    public MisEntregas misEntregas() {
        Repartidor repartidor = repartidorService.repartidorHabilitadoDelUsuarioActual();
        List<EntregaResumen> activas = asignaciones.findActivasByRepartidorId(repartidor.getId()).stream()
                .map(this::entregaResumen)
                .toList();
        List<EstadoRecorrido> abiertos = List.of(EstadoRecorrido.PLANIFICADO, EstadoRecorrido.EN_CURSO);
        List<RecorridoResumen> recorridosAbiertos = recorridos
                .findByRepartidorIdAndEstadoInOrderByIdDesc(repartidor.getId(), abiertos).stream()
                .map(this::recorridoResumen)
                .toList();
        return new MisEntregas(repartidor.getId(), activas, recorridosAbiertos);
    }

    /**
     * Ruta individual (origen restaurante -> destino del pedido) para una
     * asignación del repartidor actual. Geocodifica el destino (con caché) y
     * pide la geometría a ORS Directions (con caché por clave_hash). El criterio
     * es el del sistema (DURACION). NO afirma optimalidad.
     *
     * <p>No es read-only: {@link RutasService} persiste en caché (geocoding y
     * ruta) para no repetir llamadas a ORS.
     */
    public RutaCalculada calcularRutaIndividual(Long asignacionId) {
        Repartidor repartidor = repartidorService.repartidorHabilitadoDelUsuarioActual();
        Asignacion asignacion = asignacionPropiaActiva(asignacionId, repartidor);
        Coordenada destino = geocodificarDestino(asignacion.getPedidoClienteId());
        ResultadoRuta ruta = rutas.rutaPara(List.of(configRestaurante.origenParaRutas(), destino), CRITERIO_POR_DEFECTO);
        return RutaCalculada.de(ruta);
    }

    /**
     * Propuesta de multientrega optimizada: valida que TODAS las asignaciones
     * son del repartidor actual, activas y DOMICILIO; geocodifica cada destino;
     * pide a ORS Optimization (VROOM) el ORDEN eficiente y luego a ORS Directions
     * la geometría por carretera EN ESE ORDEN. Devuelve una PROPUESTA (no
     * persiste). Si VROOM deja paradas sin asignar, {@link RutasService}/ORS
     * lanza {@link RutasNoDisponiblesException} y NO se inventa un orden parcial.
     *
     * <p>No es read-only: geocodifica y calcula la ruta, lo que {@link RutasService}
     * persiste en caché.
     */
    public PropuestaMultientrega optimizarMultientrega(List<Long> asignacionIds, CriterioOptimizacion criterio) {
        Repartidor repartidor = repartidorService.repartidorHabilitadoDelUsuarioActual();
        CriterioOptimizacion criterioEfectivo = criterio != null ? criterio : CRITERIO_POR_DEFECTO;
        List<Long> distintos = sinDuplicados(asignacionIds);
        if (distintos.size() < 2) {
            throw new BusinessRuleException("Una multientrega necesita al menos dos pedidos distintos");
        }

        // Cada asignación: propia, activa y DOMICILIO. Geocodifica su destino.
        List<Asignacion> lista = new ArrayList<>();
        List<Coordenada> destinos = new ArrayList<>();
        List<PedidoDomicilioSPI> datos = new ArrayList<>();
        for (Long id : distintos) {
            Asignacion asignacion = asignacionPropiaActiva(id, repartidor);
            PedidoDomicilioSPI pedido = exigirDomicilio(asignacion.getPedidoClienteId());
            lista.add(asignacion);
            datos.add(pedido);
            destinos.add(geocodificar(pedido));
        }

        Coordenada origenCoord = configRestaurante.origenParaRutas();
        List<Integer> orden = rutas.optimizar(origenCoord, destinos, criterioEfectivo);

        // Reordena asignaciones/destinos/datos según el orden que devolvió VROOM.
        List<Coordenada> enOrden = new ArrayList<>();
        enOrden.add(origenCoord);
        List<PropuestaMultientrega.ParadaPropuesta> propuesta = new ArrayList<>();
        for (int i = 0; i < orden.size(); i++) {
            int idx = orden.get(i);
            Asignacion a = lista.get(idx);
            PedidoDomicilioSPI d = datos.get(idx);
            Coordenada destino = destinos.get(idx);
            enOrden.add(destino);
            propuesta.add(new PropuestaMultientrega.ParadaPropuesta(
                    i, a.getId(), a.getPedidoClienteId(), d.codigo(), d.direccion(),
                    destino != null ? destino.lat() : null,
                    destino != null ? destino.lng() : null));
        }

        ResultadoRuta ruta = rutas.rutaPara(enOrden, criterioEfectivo);
        return new PropuestaMultientrega(propuesta, criterioEfectivo.name(),
                ruta.geometria(), ruta.distanciaM(), ruta.duracionS(),
                origenCoord.lat(), origenCoord.lng());
    }

    /**
     * Crea un recorrido PLANIFICADO del repartidor actual con sus paradas en el
     * orden resuelto y enlaza cada asignación al recorrido. Valida propiedad,
     * que las asignaciones estén activas y que no estén ya en otro recorrido
     * activo. INDIVIDUAL exige exactamente una asignación; MULTIPLE al menos dos.
     */
    public RecorridoResumen crearRecorrido(TipoRecorrido tipo, List<Long> asignacionIdsEnOrden,
                                           CriterioOptimizacion criterio) {
        Repartidor repartidor = repartidorService.repartidorHabilitadoDelUsuarioActual();
        if (tipo == null) {
            throw new BusinessRuleException("El recorrido debe tener un tipo");
        }
        List<Long> distintos = sinDuplicados(asignacionIdsEnOrden);
        if (tipo == TipoRecorrido.INDIVIDUAL && distintos.size() != 1) {
            throw new BusinessRuleException("Un recorrido individual tiene exactamente un pedido");
        }
        if (tipo == TipoRecorrido.MULTIPLE && distintos.size() < 2) {
            throw new BusinessRuleException("Un recorrido múltiple necesita al menos dos pedidos");
        }

        List<Asignacion> enOrden = new ArrayList<>();
        for (Long id : distintos) {
            Asignacion asignacion = asignacionPropiaActiva(id, repartidor);
            if (asignacion.getRecorridoId() != null) {
                throw new BusinessRuleException(
                        "La asignación " + id + " ya pertenece a un recorrido");
            }
            if (asignacion.getEstado() != EstadoAsignacion.ASIGNADA) {
                throw new BusinessRuleException(
                        "Solo asignaciones aún no despachadas pueden entrar a un recorrido (estado: "
                                + asignacion.getEstado() + ")");
            }
            enOrden.add(asignacion);
        }

        CriterioOptimizacion criterioEfectivo = criterio != null ? criterio : CRITERIO_POR_DEFECTO;
        Recorrido recorrido = recorridos.saveAndFlush(
                new Recorrido(repartidor.getId(), tipo, criterioEfectivo));
        for (int i = 0; i < enOrden.size(); i++) {
            Asignacion a = enOrden.get(i);
            paradas.save(new Parada(recorrido.getId(), a.getId(), i));
            a.asignarRecorrido(recorrido.getId());
            asignaciones.save(a);
        }
        auditar("RECORRIDO_CREADO", recorrido.getId(), Map.of(
                "repartidorId", String.valueOf(repartidor.getId()),
                "tipo", tipo.name(),
                "paradas", String.valueOf(enOrden.size())));
        return recorridoResumen(recorrido);
    }

    /**
     * Inicia un recorrido PLANIFICADO del repartidor actual: lo pasa a EN_CURSO
     * (registra iniciado_at) y marca sus asignaciones EN_RUTA. Estado LOGÍSTICO:
     * no toca el estado de preparación del pedido.
     */
    public RecorridoResumen iniciarRecorrido(Long recorridoId) {
        Repartidor repartidor = repartidorService.repartidorHabilitadoDelUsuarioActual();
        Recorrido recorrido = recorridoPropio(recorridoId, repartidor);
        recorrido.iniciar();
        for (Parada parada : paradas.findByRecorridoIdOrderByOrdenAsc(recorridoId)) {
            Asignacion a = asignaciones.findById(parada.getAsignacionId()).orElseThrow();
            a.iniciarRuta();
            asignaciones.save(a);
        }
        recorridos.save(recorrido);
        auditar("RECORRIDO_INICIADO", recorrido.getId(),
                Map.of("repartidorId", String.valueOf(repartidor.getId())));
        return recorridoResumen(recorrido);
    }

    /**
     * Marca una parada como ENTREGADA (y su asignación ENTREGADA), de forma
     * IDEMPOTENTE por la pareja (parada, idempotencyKey) representada por el
     * estado: si la parada ya está ENTREGADA, NO se vuelve a aplicar y se
     * devuelve el estado actual (reintento offline del móvil). Si está en otro
     * estado terminal (FALLIDA) la doble confirmación se rechaza (422 vía
     * {@link Parada#marcarEntregada()}). El {@code idempotencyKey} se audita
     * para trazar el reintento.
     */
    public ParadaResumen marcarParadaEntregada(Long paradaId, String idempotencyKey) {
        return confirmarLlegadaRepartidor(paradaId, idempotencyKey);
    }

    /**
     * CONFIRMACIÓN DEL REPARTIDOR (1 de 2) de una entrega: el repartidor llegó/
     * entregó. Marca la parada ENTREGADA y la asignación ENTREGADA (estado
     * LOGÍSTICO) y registra la confirmación del repartidor (fecha/hora + usuario)
     * en la parada, SEPARADA del estado logístico. Es IDEMPOTENTE: si la parada
     * ya estaba ENTREGADA (confirmada por el repartidor) no re-aplica y devuelve
     * el estado actual (reintento offline del móvil); una parada FALLIDA se
     * rechaza (422). Tras confirmar, DISPARA el aviso al cliente reutilizando el
     * canal WebSocket del pedido (outbox -> STOMP) para que la app muestre
     * "tu pedido llegó, confirma la recepción". No bloquea al repartidor: puede
     * seguir con otras paradas aunque el cliente no confirme.
     */
    public ParadaResumen confirmarLlegadaRepartidor(Long paradaId, String idempotencyKey) {
        Repartidor repartidor = repartidorService.repartidorHabilitadoDelUsuarioActual();
        Parada parada = paradaPropia(paradaId, repartidor);
        // Idempotencia: una parada ya ENTREGADA (confirmada) devuelve su estado.
        if (parada.getEstado() == EstadoParada.ENTREGADA) {
            return paradaResumen(parada);
        }
        parada.confirmarPorRepartidor(actorActual());
        paradas.save(parada);
        Asignacion asignacion = asignaciones.findById(parada.getAsignacionId()).orElseThrow();
        asignacion.marcarEntregada();
        asignaciones.save(asignacion);
        auditar("PARADA_CONFIRMADA_REPARTIDOR", parada.getId(), Map.of(
                "recorridoId", String.valueOf(parada.getRecorridoId()),
                "asignacionId", String.valueOf(parada.getAsignacionId()),
                "idempotencyKey", idempotencyKey != null ? idempotencyKey : ""));
        // Avisa al cliente (reutiliza outbox -> STOMP): "el repartidor llegó".
        clientes.notificarEventoEntrega(asignacion.getPedidoClienteId());
        return paradaResumen(parada);
    }

    /**
     * CONFIRMACIÓN DEL CLIENTE (2 de 2): el cliente dueño del pedido confirma que
     * lo recibió. Valida PROPIEDAD vía la SPI de clientes (código -> id interno
     * del cliente autenticado), localiza la parada activa del pedido, exige que
     * el repartidor ya haya confirmado (si no, 422) y registra la confirmación
     * del cliente. Si existen ambas confirmaciones marca la entrega VERIFICADA.
     * Es IDEMPOTENTE: una segunda confirmación del mismo cliente no duplica. Tras
     * confirmar, notifica el nuevo estado por el canal del pedido.
     */
    public SeguimientoEntregaSPI confirmarRecepcionCliente(String codigo, String clienteUsername,
                                                           String idempotencyKey) {
        Long clienteId = clientes.resolverClienteId(clienteUsername);
        Long pedidoClienteId = clientes.pedidoClienteIdPropio(clienteId, codigo);
        Parada parada = paradas.findActivaByPedidoClienteId(pedidoClienteId)
                .orElseThrow(() -> new BusinessRuleException(
                        "El pedido aún no está en reparto: no hay entrega que confirmar"));
        boolean yaConfirmado = parada.getClienteConfirmoAt() != null;
        parada.confirmarPorCliente();
        paradas.save(parada);
        if (!yaConfirmado) {
            auditar("PARADA_CONFIRMADA_CLIENTE", parada.getId(), Map.of(
                    "pedidoClienteId", String.valueOf(pedidoClienteId),
                    "verificada", String.valueOf(parada.isEntregaVerificada()),
                    "idempotencyKey", idempotencyKey != null ? idempotencyKey : ""));
            clientes.notificarEventoEntrega(pedidoClienteId);
        }
        return seguimientoDeParada(parada);
    }

    /**
     * ADMIN: paradas con el repartidor confirmado pero pendientes de la
     * confirmación del cliente (entrega aún no verificada). Permite al admin
     * revisar incidencias de confirmación.
     */
    @Transactional(readOnly = true)
    public List<ParadaResumen> listarPendientesConfirmacionCliente() {
        return paradas.buscarPendientesConfirmacionCliente().stream()
                .map(this::paradaResumen)
                .toList();
    }

    /**
     * ADMIN: resuelve de forma AUDITADA y JUSTIFICADA una entrega cuyo cliente
     * nunca confirmó. Exige un motivo, marca la entrega VERIFICADA registrando
     * quién la resolvió y por qué. NO hay cierre automático por tiempo.
     */
    public ParadaResumen resolverConfirmacionPendiente(Long paradaId, String motivo) {
        Parada parada = paradas.findById(paradaId)
                .orElseThrow(() -> new NotFoundException("La parada no existe"));
        parada.resolverPorAdmin(actorActual(), motivo);
        paradas.save(parada);
        auditar("PARADA_CONFIRMACION_RESUELTA_ADMIN", parada.getId(), Map.of(
                "asignacionId", String.valueOf(parada.getAsignacionId()),
                "motivo", motivo != null ? motivo : ""));
        return paradaResumen(parada);
    }

    /**
     * Seguimiento de la entrega de un pedido para la barra de progreso del
     * cliente: valida propiedad (el pedido es del cliente autenticado) y expone
     * la fase logística y la fase de confirmación sin PII.
     */
    public SeguimientoEntregaSPI seguimientoDePedido(String codigo, String clienteUsername) {
        Long clienteId = clientes.resolverClienteId(clienteUsername);
        Long pedidoClienteId = clientes.pedidoClienteIdPropio(clienteId, codigo);
        return paradas.findActivaByPedidoClienteId(pedidoClienteId)
                .map(this::seguimientoDeParada)
                .orElseGet(() -> seguimientoSoloAsignacion(pedidoClienteId));
    }

    /**
     * Registra una incidencia de entrega (parada PENDIENTE), conservando la
     * trazabilidad: crea {@link Incidencia}, marca la parada FALLIDA y la
     * asignación FALLIDA. La incidencia NO se borra.
     */
    public ParadaResumen registrarIncidencia(Long paradaId, String motivo) {
        Repartidor repartidor = repartidorService.repartidorHabilitadoDelUsuarioActual();
        if (motivo == null || motivo.isBlank()) {
            throw new BusinessRuleException("La incidencia debe tener un motivo");
        }
        Parada parada = paradaPropia(paradaId, repartidor);
        parada.marcarFallida();
        paradas.save(parada);
        incidencias.save(new Incidencia(parada.getId(), motivo, actorActual()));
        Asignacion asignacion = asignaciones.findById(parada.getAsignacionId()).orElseThrow();
        asignacion.marcarFallida();
        asignaciones.save(asignacion);
        auditar("PARADA_INCIDENCIA", parada.getId(), Map.of(
                "recorridoId", String.valueOf(parada.getRecorridoId()),
                "asignacionId", String.valueOf(parada.getAsignacionId()),
                "motivo", motivo));
        return paradaResumen(parada);
    }

    /**
     * Finaliza un recorrido EN_CURSO del repartidor actual, SOLO si todas sus
     * paradas están resueltas (ENTREGADA o FALLIDA). Si queda alguna PENDIENTE
     * -> 422.
     */
    public RecorridoResumen finalizarRecorrido(Long recorridoId) {
        Repartidor repartidor = repartidorService.repartidorHabilitadoDelUsuarioActual();
        Recorrido recorrido = recorridoPropio(recorridoId, repartidor);
        if (paradas.existsByRecorridoIdAndEstado(recorridoId, EstadoParada.PENDIENTE)) {
            throw new BusinessRuleException(
                    "No se puede finalizar: hay paradas pendientes de resolver");
        }
        recorrido.finalizar();
        recorridos.save(recorrido);
        auditar("RECORRIDO_FINALIZADO", recorrido.getId(),
                Map.of("repartidorId", String.valueOf(repartidor.getId())));
        return recorridoResumen(recorrido);
    }

    /**
     * Recalcula la ruta de las paradas PENDIENTES restantes desde la ubicación
     * ACTUAL del repartidor (excluye ENTREGADA/FALLIDA). SOLO cuando se pide (no
     * automático). Reutiliza la caché por clave_hash cuando coincide, para no
     * gastar cuota de ORS innecesariamente. NO afirma optimalidad.
     *
     * <p>No es read-only: puede persistir en caché una ruta nueva.
     */
    public RutaCalculada recalcularDesde(Long recorridoId, java.math.BigDecimal latActual,
                                         java.math.BigDecimal lngActual) {
        Repartidor repartidor = repartidorService.repartidorHabilitadoDelUsuarioActual();
        Recorrido recorrido = recorridoPropio(recorridoId, repartidor);
        if (recorrido.getEstado() != EstadoRecorrido.EN_CURSO) {
            throw new BusinessRuleException("Solo se recalcula un recorrido en curso");
        }
        List<Coordenada> enOrden = new ArrayList<>();
        enOrden.add(new Coordenada(latActual, lngActual));
        for (Parada parada : paradas.findByRecorridoIdOrderByOrdenAsc(recorridoId)) {
            if (parada.getEstado() == EstadoParada.PENDIENTE) {
                Asignacion a = asignaciones.findById(parada.getAsignacionId()).orElseThrow();
                enOrden.add(geocodificarDestino(a.getPedidoClienteId()));
            }
        }
        if (enOrden.size() < 2) {
            throw new BusinessRuleException("No quedan paradas pendientes por recalcular");
        }
        ResultadoRuta ruta = rutas.rutaPara(enOrden, recorrido.getCriterioOptimizacion());
        return RutaCalculada.de(ruta);
    }

    /** Recorridos FINALIZADOS/CANCELADOS del repartidor actual. */
    @Transactional(readOnly = true)
    public List<RecorridoResumen> historial() {
        Repartidor repartidor = repartidorService.repartidorHabilitadoDelUsuarioActual();
        List<EstadoRecorrido> cerrados = List.of(EstadoRecorrido.FINALIZADO, EstadoRecorrido.CANCELADO);
        return recorridos.findByRepartidorIdAndEstadoInOrderByIdDesc(repartidor.getId(), cerrados).stream()
                .map(this::recorridoResumen)
                .toList();
    }

    // ---- verificación de propiedad ----

    private Asignacion asignacionPropiaActiva(Long asignacionId, Repartidor repartidor) {
        Asignacion asignacion = asignaciones.findById(asignacionId)
                .orElseThrow(() -> new NotFoundException("La asignación no existe"));
        if (!asignacion.getRepartidorId().equals(repartidor.getId())) {
            // No revelar si existe o no: el repartidor no es dueño.
            throw new AccessDeniedException("La asignación no pertenece al repartidor");
        }
        if (asignacion.getEstado() == EstadoAsignacion.REASIGNADA
                || asignacion.getEstado() == EstadoAsignacion.FALLIDA) {
            throw new BusinessRuleException(
                    "La asignación ya no está activa (estado: " + asignacion.getEstado() + ")");
        }
        return asignacion;
    }

    private Recorrido recorridoPropio(Long recorridoId, Repartidor repartidor) {
        Recorrido recorrido = recorridos.findById(recorridoId)
                .orElseThrow(() -> new NotFoundException("El recorrido no existe"));
        if (!recorrido.getRepartidorId().equals(repartidor.getId())) {
            throw new AccessDeniedException("El recorrido no pertenece al repartidor");
        }
        return recorrido;
    }

    private Parada paradaPropia(Long paradaId, Repartidor repartidor) {
        Parada parada = paradas.findById(paradaId)
                .orElseThrow(() -> new NotFoundException("La parada no existe"));
        Recorrido recorrido = recorridos.findById(parada.getRecorridoId())
                .orElseThrow(() -> new NotFoundException("El recorrido de la parada no existe"));
        if (!recorrido.getRepartidorId().equals(repartidor.getId())) {
            throw new AccessDeniedException("La parada no pertenece al repartidor");
        }
        return parada;
    }

    // ---- geocoding de destinos ----

    private Coordenada geocodificarDestino(Long pedidoClienteId) {
        return geocodificar(exigirPedido(pedidoClienteId));
    }

    private Coordenada geocodificar(PedidoDomicilioSPI pedido) {
        if (pedido.direccionId() == null || pedido.direccion() == null || pedido.direccion().isBlank()) {
            throw new BusinessRuleException(
                    "El pedido " + pedido.pedidoClienteId() + " no tiene una dirección para geocodificar");
        }
        return rutas.geocodificar(pedido.direccionId(), pedido.direccion())
                .orElseThrow(() -> new BusinessRuleException(
                        "No se pudo geocodificar la dirección del pedido " + pedido.pedidoClienteId()));
    }

    private PedidoDomicilioSPI exigirPedido(Long pedidoClienteId) {
        return clientes.datosEntrega(pedidoClienteId)
                .orElseThrow(() -> new NotFoundException("El pedido no existe"));
    }

    private PedidoDomicilioSPI exigirDomicilio(Long pedidoClienteId) {
        PedidoDomicilioSPI pedido = exigirPedido(pedidoClienteId);
        if (!"DOMICILIO".equals(pedido.metodoEntrega())) {
            throw new BusinessRuleException(
                    "El pedido " + pedidoClienteId + " no es para reparto a domicilio");
        }
        return pedido;
    }

    // ---- mapeos ----

    private EntregaResumen entregaResumen(Asignacion a) {
        PedidoDomicilioSPI datos = clientes.datosEntrega(a.getPedidoClienteId()).orElse(null);
        return new EntregaResumen(
                a.getId(),
                a.getPedidoClienteId(),
                a.getRecorridoId(),
                a.getEstado().name(),
                datos != null ? datos.codigo() : null,
                datos != null ? datos.clienteNombre() : null,
                datos != null ? datos.direccion() : null,
                datos != null ? datos.telefono() : null);
    }

    private RecorridoResumen recorridoResumen(Recorrido recorrido) {
        List<ParadaResumen> lista = paradas.findByRecorridoIdOrderByOrdenAsc(recorrido.getId()).stream()
                .map(this::paradaResumen)
                .toList();
        return RecorridoResumen.de(recorrido, origenOpcional(), lista);
    }

    /**
     * Seguimiento derivado de una parada: fase logística a partir del estado de
     * la asignación/parada y fase de confirmación a partir de las dos
     * confirmaciones. Conceptos separados, sin PII.
     */
    private SeguimientoEntregaSPI seguimientoDeParada(Parada parada) {
        Asignacion a = asignaciones.findById(parada.getAsignacionId()).orElse(null);
        return new SeguimientoEntregaSPI(
                true,
                faseLogistica(a, parada).name(),
                parada.faseConfirmacion().name(),
                parada.getRepartidorConfirmoAt(),
                parada.getClienteConfirmoAt(),
                parada.isEntregaVerificada());
    }

    /** Seguimiento cuando hay asignación pero aún no parada (sin recorrido). */
    private SeguimientoEntregaSPI seguimientoSoloAsignacion(Long pedidoClienteId) {
        Asignacion a = asignaciones.findActivaByPedidoClienteId(pedidoClienteId).orElse(null);
        if (a == null) {
            return SeguimientoEntregaSPI.sinEntrega();
        }
        FaseLogistica fase = a.getEstado() == EstadoAsignacion.EN_RUTA
                ? FaseLogistica.EN_CAMINO : FaseLogistica.ASIGNADO;
        return new SeguimientoEntregaSPI(true, fase.name(),
                FaseConfirmacion.SIN_CONFIRMAR.name(), null, null, false);
    }

    /**
     * Fase logística de una parada, derivada SIN tocar la máquina de estados:
     * FALLIDA si la parada falló; ENTREGADO_POR_REPARTIDOR si el repartidor
     * confirmó; EN_CAMINO si la asignación está EN_RUTA; ASIGNADO en otro caso.
     */
    private FaseLogistica faseLogistica(Asignacion a, Parada parada) {
        if (parada.getEstado() == EstadoParada.FALLIDA) {
            return FaseLogistica.FALLIDA;
        }
        if (parada.getRepartidorConfirmoAt() != null) {
            return FaseLogistica.ENTREGADO_POR_REPARTIDOR;
        }
        if (a != null && a.getEstado() == EstadoAsignacion.EN_RUTA) {
            return FaseLogistica.EN_CAMINO;
        }
        return FaseLogistica.ASIGNADO;
    }

    /** Fase logística del reparto para el seguimiento del cliente (sin PII). */
    private enum FaseLogistica {
        SIN_ASIGNAR, ASIGNADO, EN_CAMINO, ENTREGADO_POR_REPARTIDOR, FALLIDA
    }

    private ParadaResumen paradaResumen(Parada parada) {
        Asignacion a = asignaciones.findById(parada.getAsignacionId()).orElse(null);
        EntregaResumen entrega = a != null ? entregaResumen(a) : null;
        return ParadaResumen.de(parada, entrega, coordDeParada(a));
    }

    /**
     * Coordenada geocodificada del destino de la parada, reutilizando la caché
     * de geocoding. Devuelve {@code null} (sin romper) si no hay asignación, el
     * pedido no tiene dirección o el geocoding no la resuelve: la parada se
     * muestra sin posición en el mapa.
     */
    private Coordenada coordDeParada(Asignacion a) {
        if (a == null) {
            return null;
        }
        try {
            PedidoDomicilioSPI pedido = clientes.datosEntrega(a.getPedidoClienteId()).orElse(null);
            if (pedido == null || pedido.direccionId() == null
                    || pedido.direccion() == null || pedido.direccion().isBlank()) {
                return null;
            }
            return rutas.geocodificar(pedido.direccionId(), pedido.direccion()).orElse(null);
        } catch (RuntimeException e) {
            return null;
        }
    }

    /**
     * Origen de reparto (restaurante) si está configurado; {@code null} si no,
     * para no romper las respuestas del recorrido cuando falta configuración.
     */
    private Coordenada origenOpcional() {
        return configRestaurante.origenConfigurado() ? configRestaurante.origenParaRutas() : null;
    }

    private List<Long> sinDuplicados(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessRuleException("Debe indicar al menos una asignación");
        }
        List<Long> distintos = ids.stream().distinct().toList();
        if (distintos.size() != ids.size()) {
            throw new BusinessRuleException("La lista de asignaciones no puede tener duplicados");
        }
        return distintos;
    }

    private String actorActual() {
        org.springframework.security.core.Authentication auth =
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "sistema";
    }

    private void auditar(String tipo, Long recursoId, Map<String, String> detalle) {
        Map<String, String> ordenado = new LinkedHashMap<>(detalle);
        auditoria.registrar(tipo, "RECORRIDO", String.valueOf(recursoId),
                json(ordenado), CATEGORIA_AUDITORIA);
    }

    private String json(Map<String, String> detalle) {
        try {
            return objectMapper.writeValueAsString(detalle);
        } catch (Exception e) {
            return "{\"detalle\":\"no serializable\"}";
        }
    }
}
