package com.restaurante.delivery.web;

import com.restaurante.delivery.application.RepartidorResumen;
import com.restaurante.delivery.application.RepartidorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Administración del estado operativo de los repartidores (lo usa el ADMIN).
 * Separado del flujo operativo del repartidor (fases siguientes): aquí el
 * administrador habilita/suspende y consulta.
 *
 * <p>Autorización por permiso {@code delivery:repartidores-gestionar}.
 * Habilitar solo aplica a usuarios con rol DELIVERY; en caso contrario la
 * capa de aplicación responde 422 (regla de negocio).
 */
@RestController
@RequestMapping("/api/v1/delivery/repartidores")
@Tag(name = "delivery-admin", description = "Administración de repartidores (habilitar/suspender/listar)")
public class DeliveryAdminController {

    private final RepartidorService repartidores;

    public DeliveryAdminController(RepartidorService repartidores) {
        this.repartidores = repartidores;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('delivery:repartidores-gestionar')")
    @Operation(summary = "Listar repartidores con su estado operativo")
    public List<RepartidorResponse> listar() {
        return repartidores.listarRepartidores().stream()
                .map(RepartidorResponse::de)
                .toList();
    }

    @GetMapping("/habilitados")
    @PreAuthorize("hasAuthority('delivery:repartidores-gestionar')")
    @Operation(summary = "Listar solo repartidores HABILITADOS")
    public List<RepartidorResponse> habilitados() {
        return repartidores.listarHabilitados().stream()
                .map(RepartidorResponse::de)
                .toList();
    }

    @PostMapping("/{usuarioId}/habilitar")
    @PreAuthorize("hasAuthority('delivery:repartidores-gestionar')")
    @Operation(summary = "Habilitar a un repartidor (solo usuarios con rol DELIVERY)")
    public RepartidorResponse habilitar(@PathVariable Long usuarioId) {
        return RepartidorResponse.de(repartidores.habilitar(usuarioId, actorActual()));
    }

    @PostMapping("/{usuarioId}/suspender")
    @PreAuthorize("hasAuthority('delivery:repartidores-gestionar')")
    @Operation(summary = "Suspender a un repartidor habilitado")
    public RepartidorResponse suspender(@PathVariable Long usuarioId) {
        return RepartidorResponse.de(repartidores.suspender(usuarioId, actorActual()));
    }

    private String actorActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "sistema";
    }

    /** Respuesta de un repartidor; no expone la entidad JPA. */
    public record RepartidorResponse(
            Long id,
            Long usuarioId,
            String estado,
            String habilitadoPor,
            String habilitadoAt) {

        static RepartidorResponse de(RepartidorResumen r) {
            return new RepartidorResponse(
                    r.id(),
                    r.usuarioId(),
                    r.estado().name(),
                    r.habilitadoPor(),
                    r.habilitadoAt() != null ? r.habilitadoAt().toString() : null);
        }
    }
}
