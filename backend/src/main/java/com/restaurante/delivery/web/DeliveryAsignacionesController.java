package com.restaurante.delivery.web;

import com.restaurante.delivery.application.AsignacionResumen;
import com.restaurante.delivery.application.AsignacionService;
import com.restaurante.delivery.application.PedidoListoParaAsignar;
import com.restaurante.delivery.domain.EstadoAsignacion;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Administración de asignaciones de pedidos a domicilio a repartidores (lo usa
 * el ADMIN). Asigna, reasigna y consulta los estados de reparto.
 *
 * <p>Autorización por permiso {@code delivery:asignaciones-gestionar}: solo el
 * administrador ve los datos de entrega (PII) de los pedidos. El estado
 * logístico de la asignación es independiente del estado de preparación del
 * pedido en comercial.
 */
@RestController
@RequestMapping("/api/v1/delivery/asignaciones")
@Tag(name = "delivery-asignaciones",
        description = "Asignación de pedidos a domicilio a repartidores (asignar/reasignar/consultar)")
public class DeliveryAsignacionesController {

    private final AsignacionService asignaciones;

    public DeliveryAsignacionesController(AsignacionService asignaciones) {
        this.asignaciones = asignaciones;
    }

    @GetMapping("/pendientes-de-asignar")
    @PreAuthorize("hasAuthority('delivery:asignaciones-gestionar')")
    @Operation(summary = "Pedidos a domicilio listos y sin asignación activa")
    public List<PedidoListoResponse> pendientesDeAsignar() {
        return asignaciones.listarPedidosListosParaAsignar().stream()
                .map(PedidoListoResponse::de)
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('delivery:asignaciones-gestionar')")
    @Operation(summary = "Asignar un pedido a un repartidor habilitado")
    public AsignacionResponse asignar(@Valid @RequestBody AsignarRequest request) {
        return AsignacionResponse.de(
                asignaciones.asignar(request.pedidoClienteId(), request.repartidorId(), actorActual()));
    }

    @PostMapping("/lote")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('delivery:asignaciones-gestionar')")
    @Operation(summary = "Asignar varios pedidos al mismo repartidor (multientrega)")
    public List<AsignacionResponse> asignarLote(@Valid @RequestBody AsignarLoteRequest request) {
        return asignaciones.asignarVarios(request.pedidoClienteIds(), request.repartidorId(), actorActual())
                .stream()
                .map(AsignacionResponse::de)
                .toList();
    }

    @PostMapping("/{pedidoClienteId}/reasignar")
    @PreAuthorize("hasAuthority('delivery:asignaciones-gestionar')")
    @Operation(summary = "Reasignar un pedido a otro repartidor")
    public AsignacionResponse reasignar(@PathVariable Long pedidoClienteId,
                                        @Valid @RequestBody ReasignarRequest request) {
        return AsignacionResponse.de(
                asignaciones.reasignar(pedidoClienteId, request.repartidorId(), actorActual()));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('delivery:asignaciones-gestionar')")
    @Operation(summary = "Estados de reparto (filtros opcionales: estado, repartidorId)")
    public List<AsignacionResponse> listar(@RequestParam(required = false) EstadoAsignacion estado,
                                           @RequestParam(required = false) Long repartidorId) {
        return asignaciones.listarAsignaciones(estado, repartidorId).stream()
                .map(AsignacionResponse::de)
                .toList();
    }

    private String actorActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "sistema";
    }

    // ---- DTOs ----

    /** Alta de una asignación individual. */
    public record AsignarRequest(
            @NotNull Long pedidoClienteId,
            @NotNull Long repartidorId) {
    }

    /** Alta en lote (multientrega): varios pedidos al mismo repartidor. */
    public record AsignarLoteRequest(
            @NotEmpty List<@NotNull Long> pedidoClienteIds,
            @NotNull Long repartidorId) {
    }

    /** Reasignación al repartidor indicado. */
    public record ReasignarRequest(
            @NotNull Long repartidorId) {
    }

    /** Pedido listo para asignar; incluye datos de entrega (solo admin). */
    public record PedidoListoResponse(
            Long pedidoClienteId,
            String codigo,
            String clienteNombre,
            String direccion,
            String telefono) {

        static PedidoListoResponse de(PedidoListoParaAsignar p) {
            return new PedidoListoResponse(
                    p.pedidoClienteId(), p.codigo(), p.clienteNombre(), p.direccion(), p.telefono());
        }
    }

    /** Estado de reparto de una asignación; no expone la entidad JPA. */
    public record AsignacionResponse(
            Long asignacionId,
            Long pedidoClienteId,
            Long repartidorId,
            String estado,
            String asignadoPor,
            String asignadoAt,
            String codigo,
            String clienteNombre,
            String direccion,
            String telefono) {

        static AsignacionResponse de(AsignacionResumen a) {
            return new AsignacionResponse(
                    a.asignacionId(),
                    a.pedidoClienteId(),
                    a.repartidorId(),
                    a.estado(),
                    a.asignadoPor(),
                    a.asignadoAt() != null ? a.asignadoAt().toString() : null,
                    a.codigo(),
                    a.clienteNombre(),
                    a.direccion(),
                    a.telefono());
        }
    }
}
