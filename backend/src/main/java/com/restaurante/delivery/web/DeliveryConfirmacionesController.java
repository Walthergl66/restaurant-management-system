package com.restaurante.delivery.web;

import com.restaurante.delivery.application.ParadaResumen;
import com.restaurante.delivery.application.RecorridoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

/**
 * ADMIN: incidencias de la DOBLE CONFIRMACIÓN de entrega. Permite consultar las
 * entregas que el repartidor ya confirmó pero cuyo cliente nunca confirmó, y
 * resolverlas de forma AUDITADA y justificada (con motivo). No hay cierre
 * automático por tiempo: solo esta acción humana cierra una entrega pendiente.
 *
 * <p>Autorización: {@code delivery:confirmacion-resolver} (permiso propio del
 * admin, separado de la gestión de asignaciones). Las respuestas no exponen la
 * entidad JPA.
 */
@RestController
@RequestMapping("/api/v1/delivery/confirmaciones")
@Tag(name = "delivery-confirmaciones",
        description = "Admin: entregas pendientes de confirmación del cliente y su resolución auditada")
public class DeliveryConfirmacionesController {

    private final RecorridoService recorridos;

    public DeliveryConfirmacionesController(RecorridoService recorridos) {
        this.recorridos = recorridos;
    }

    @GetMapping("/pendientes")
    @PreAuthorize("hasAuthority('delivery:confirmacion-resolver')")
    @Operation(summary = "Entregas confirmadas por el repartidor y pendientes de confirmación del cliente")
    public List<ConfirmacionPendienteResponse> pendientes() {
        return recorridos.listarPendientesConfirmacionCliente().stream()
                .map(ConfirmacionPendienteResponse::de)
                .toList();
    }

    @PostMapping("/{paradaId}/resolver")
    @PreAuthorize("hasAuthority('delivery:confirmacion-resolver')")
    @Operation(summary = "Resolver (auditada, con motivo) una entrega pendiente de confirmación del cliente")
    public ConfirmacionPendienteResponse resolver(@PathVariable Long paradaId,
                                                  @Valid @RequestBody ResolverRequest request) {
        return ConfirmacionPendienteResponse.de(
                recorridos.resolverConfirmacionPendiente(paradaId, request.motivo()));
    }

    /** Motivo justificado de la resolución administrativa (obligatorio). */
    public record ResolverRequest(@NotBlank String motivo) {
    }

    /** Vista de una entrega pendiente/resuelta de confirmación; sin entidad JPA. */
    public record ConfirmacionPendienteResponse(
            Long paradaId,
            Long asignacionId,
            Long pedidoClienteId,
            String codigo,
            String direccion,
            String estado,
            String faseConfirmacion,
            Instant repartidorConfirmoAt,
            Instant clienteConfirmoAt,
            boolean entregaVerificada) {

        static ConfirmacionPendienteResponse de(ParadaResumen p) {
            return new ConfirmacionPendienteResponse(
                    p.paradaId(), p.asignacionId(), p.pedidoClienteId(), p.codigo(),
                    p.direccion(), p.estado(), p.faseConfirmacion(),
                    p.repartidorConfirmoAt(), p.clienteConfirmoAt(), p.entregaVerificada());
        }
    }
}
