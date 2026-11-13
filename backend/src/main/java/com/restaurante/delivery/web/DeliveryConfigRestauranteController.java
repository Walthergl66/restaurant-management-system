package com.restaurante.delivery.web;

import com.restaurante.delivery.application.ConfigRestauranteResumen;
import com.restaurante.delivery.application.ConfigRestauranteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

/**
 * Configuración de la ubicación del restaurante (panel ADMIN): el ORIGEN de las
 * rutas de reparto, persistido y editable. Autorización por permiso
 * {@code delivery:config-gestionar}.
 *
 * <p>Flujo: el administrador introduce la dirección (se intenta geocodificar con
 * ORS) y, si ORS no resuelve un punto claro, el resumen expone
 * {@code requiereSeleccionManual=true} para que marque el punto en el mapa y lo
 * fije con el endpoint de coordenadas. No se inventan coordenadas.
 */
@RestController
@RequestMapping("/api/v1/delivery/config-restaurante")
@Tag(name = "delivery-config-restaurante",
        description = "Ubicación del restaurante: origen persistido de las rutas de reparto")
public class DeliveryConfigRestauranteController {

    private final ConfigRestauranteService config;

    public DeliveryConfigRestauranteController(ConfigRestauranteService config) {
        this.config = config;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('delivery:config-gestionar')")
    @Operation(summary = "Consultar la configuración de la ubicación del restaurante")
    public ConfigRestauranteResumen obtener() {
        return config.obtenerConfig();
    }

    @PutMapping("/direccion")
    @PreAuthorize("hasAuthority('delivery:config-gestionar')")
    @Operation(summary = "Actualizar la dirección e intentar geocodificarla con ORS")
    public ConfigRestauranteResumen actualizarDireccion(@Valid @RequestBody DireccionRequest request) {
        return config.actualizarDireccion(request.direccion(), actorActual());
    }

    @PutMapping("/coordenadas")
    @PreAuthorize("hasAuthority('delivery:config-gestionar')")
    @Operation(summary = "Fijar las coordenadas marcadas por el administrador en el mapa (MANUAL)")
    public ConfigRestauranteResumen fijarCoordenadas(@Valid @RequestBody CoordenadasRequest request) {
        return config.fijarCoordenadasManual(request.lat(), request.lng(), actorActual());
    }

    private String actorActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "sistema";
    }

    /** Cuerpo de actualización de la dirección del establecimiento. */
    public record DireccionRequest(
            @NotBlank @Size(max = 200) String direccion) {
    }

    /** Cuerpo con las coordenadas que el administrador marcó en el mapa. */
    public record CoordenadasRequest(
            @NotNull @DecimalMin("-90") @DecimalMax("90") BigDecimal lat,
            @NotNull @DecimalMin("-180") @DecimalMax("180") BigDecimal lng) {
    }
}
