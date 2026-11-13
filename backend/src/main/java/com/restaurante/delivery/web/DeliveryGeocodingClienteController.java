package com.restaurante.delivery.web;

import com.restaurante.delivery.application.GeocodingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

/**
 * App del CLIENTE: búsqueda de direcciones (geocoding) para colocar el marcador
 * en el mapa del checkout. Vive en el módulo delivery porque es quien posee el
 * cliente de ORS y su api-key; el móvil llama a este endpoint del backend en
 * lugar de a ORS directamente, de modo que la api-key NUNCA viaja al cliente.
 *
 * <p>Ruta bajo {@code /api/v1/clientes/...} (como la confirmación de recepción)
 * para que la app del cliente la consuma junto al resto de su flujo. Devuelve
 * de 0 a N candidatos {descripcion, lat, lng}; lista vacía si no hay resultados
 * (el cliente ajusta el marcador manualmente), nunca 500 por "sin resultados".
 */
@RestController
@RequestMapping("/api/v1/clientes")
@Tag(name = "cliente-geocoding",
        description = "Cliente: búsqueda de direcciones para el mapa del checkout")
public class DeliveryGeocodingClienteController {

    private final GeocodingService geocoding;

    public DeliveryGeocodingClienteController(GeocodingService geocoding) {
        this.geocoding = geocoding;
    }

    /** Candidato devuelto al cliente: descripción legible + coordenadas. */
    public record CandidatoDireccionResponse(String descripcion, BigDecimal lat, BigDecimal lng) {
    }

    @GetMapping("/geocodificar")
    @PreAuthorize("hasAuthority('clientes:carrito-gestionar')")
    @Operation(summary = "Buscar direcciones por texto (geocoding) para el checkout")
    public List<CandidatoDireccionResponse> geocodificar(@RequestParam String texto) {
        return geocoding.buscar(texto, 5).stream()
                .map(c -> new CandidatoDireccionResponse(c.descripcion(), c.lat(), c.lng()))
                .toList();
    }
}
