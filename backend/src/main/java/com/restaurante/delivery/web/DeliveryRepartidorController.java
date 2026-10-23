package com.restaurante.delivery.web;

import com.restaurante.delivery.application.MisEntregas;
import com.restaurante.delivery.application.PropuestaMultientrega;
import com.restaurante.delivery.application.RecorridoResumen;
import com.restaurante.delivery.application.RecorridoService;
import com.restaurante.delivery.application.RutaCalculada;
import com.restaurante.delivery.domain.CriterioOptimizacion;
import com.restaurante.delivery.domain.TipoRecorrido;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

/**
 * API REST del REPARTIDOR (app móvil): rutas, optimización del orden de paradas
 * y recorridos. El móvil siempre pasa por NUESTRO backend; nunca llama a ORS
 * directamente (la api-key vive solo aquí).
 *
 * <p>Autorización en dos capas: el permiso ({@code delivery:mis-entregas} para
 * consultar/calcular, {@code delivery:recorrido-operar} para operar) y, dentro
 * del servicio, el guard de repartidor HABILITADO + la verificación de
 * PROPIEDAD de cada recurso. Un repartidor no puede operar lo de otro.
 *
 * <p>Si el proveedor de rutas (ORS) no está disponible, los endpoints que lo
 * necesitan responden 503 (lo mapea el {@code @RestControllerAdvice} global).
 */
@RestController
@RequestMapping("/api/v1/delivery")
@Tag(name = "delivery-repartidor",
        description = "Operación del repartidor: entregas, rutas, optimización y recorridos")
public class DeliveryRepartidorController {

    private final RecorridoService recorridos;

    public DeliveryRepartidorController(RecorridoService recorridos) {
        this.recorridos = recorridos;
    }

    @GetMapping("/mis-entregas")
    @PreAuthorize("hasAuthority('delivery:mis-entregas')")
    @Operation(summary = "Asignaciones activas y recorridos abiertos del repartidor autenticado")
    public MisEntregas misEntregas() {
        return recorridos.misEntregas();
    }

    @PostMapping("/rutas/individual")
    @PreAuthorize("hasAuthority('delivery:mis-entregas')")
    @Operation(summary = "Ruta individual (restaurante -> destino) para una asignación propia")
    public RutaCalculada rutaIndividual(@Valid @RequestBody RutaIndividualRequest request) {
        return recorridos.calcularRutaIndividual(request.asignacionId());
    }

    @PostMapping("/rutas/optimizar")
    @PreAuthorize("hasAuthority('delivery:mis-entregas')")
    @Operation(summary = "Propuesta de multientrega con el orden de paradas optimizado por ORS")
    public PropuestaMultientrega optimizar(@Valid @RequestBody OptimizarRequest request) {
        return recorridos.optimizarMultientrega(request.asignacionIds(), request.criterio());
    }

    @PostMapping("/recorridos")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('delivery:recorrido-operar')")
    @Operation(summary = "Crear un recorrido (INDIVIDUAL o MULTIPLE) con las paradas en orden")
    public RecorridoResumen crear(@Valid @RequestBody CrearRecorridoRequest request) {
        return recorridos.crearRecorrido(request.tipo(), request.asignacionIds(), request.criterio());
    }

    @PostMapping("/recorridos/{id}/iniciar")
    @PreAuthorize("hasAuthority('delivery:recorrido-operar')")
    @Operation(summary = "Iniciar un recorrido planificado (lo pasa a EN_CURSO)")
    public RecorridoResumen iniciar(@PathVariable Long id) {
        return recorridos.iniciarRecorrido(id);
    }

    @PostMapping("/recorridos/{id}/recalcular")
    @PreAuthorize("hasAuthority('delivery:recorrido-operar')")
    @Operation(summary = "Recalcular la ruta de las paradas pendientes desde la ubicación actual")
    public RutaCalculada recalcular(@PathVariable Long id, @Valid @RequestBody RecalcularRequest request) {
        return recorridos.recalcularDesde(id, request.lat(), request.lng());
    }

    @PostMapping("/paradas/{id}/entregada")
    @PreAuthorize("hasAuthority('delivery:recorrido-operar')")
    @Operation(summary = "Confirmación del repartidor: marca la parada entregada (idempotente por Idempotency-Key)")
    public ParadaResponse entregada(@PathVariable Long id,
                                    @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        return ParadaResponse.de(recorridos.confirmarLlegadaRepartidor(id, idempotencyKey));
    }

    @PostMapping("/paradas/{id}/he-llegado")
    @PreAuthorize("hasAuthority('delivery:recorrido-operar')")
    @Operation(summary = "Alias de la confirmación del repartidor (he llegado / entrega realizada)")
    public ParadaResponse heLlegado(@PathVariable Long id,
                                    @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        return ParadaResponse.de(recorridos.confirmarLlegadaRepartidor(id, idempotencyKey));
    }

    @PostMapping("/paradas/{id}/incidencia")
    @PreAuthorize("hasAuthority('delivery:recorrido-operar')")
    @Operation(summary = "Registrar una incidencia de entrega (marca la parada FALLIDA)")
    public ParadaResponse incidencia(@PathVariable Long id, @Valid @RequestBody IncidenciaRequest request) {
        return ParadaResponse.de(recorridos.registrarIncidencia(id, request.motivo()));
    }

    @PostMapping("/recorridos/{id}/finalizar")
    @PreAuthorize("hasAuthority('delivery:recorrido-operar')")
    @Operation(summary = "Finalizar un recorrido en curso (solo si todas las paradas están resueltas)")
    public RecorridoResumen finalizar(@PathVariable Long id) {
        return recorridos.finalizarRecorrido(id);
    }

    @GetMapping("/recorridos/historial")
    @PreAuthorize("hasAuthority('delivery:mis-entregas')")
    @Operation(summary = "Recorridos finalizados/cancelados del repartidor autenticado")
    public List<RecorridoResumen> historial() {
        return recorridos.historial();
    }

    // ---- DTOs de request ----

    /** Ruta individual para una asignación propia. */
    public record RutaIndividualRequest(@NotNull Long asignacionId) {
    }

    /** Propuesta de multientrega: asignaciones a optimizar y criterio opcional
     *  (por defecto DURACION, la ruta más rápida). */
    public record OptimizarRequest(
            @NotEmpty List<@NotNull Long> asignacionIds,
            CriterioOptimizacion criterio) {
    }

    /** Alta de recorrido con las asignaciones en el orden resuelto. */
    public record CrearRecorridoRequest(
            @NotNull TipoRecorrido tipo,
            @NotEmpty List<@NotNull Long> asignacionIds,
            CriterioOptimizacion criterio) {
    }

    /** Ubicación actual del repartidor para recalcular. */
    public record RecalcularRequest(
            @NotNull BigDecimal lat,
            @NotNull BigDecimal lng) {
    }

    /** Motivo de una incidencia de entrega. */
    public record IncidenciaRequest(@NotBlank String motivo) {
    }

    // ---- DTO de response ----

    /** Estado de una parada tras operarla; no expone la entidad JPA. */
    public record ParadaResponse(
            Long paradaId,
            Long asignacionId,
            Long pedidoClienteId,
            int orden,
            String estado,
            String codigo,
            String direccion) {

        static ParadaResponse de(com.restaurante.delivery.application.ParadaResumen p) {
            return new ParadaResponse(
                    p.paradaId(), p.asignacionId(), p.pedidoClienteId(),
                    p.orden(), p.estado(), p.codigo(), p.direccion());
        }
    }
}
