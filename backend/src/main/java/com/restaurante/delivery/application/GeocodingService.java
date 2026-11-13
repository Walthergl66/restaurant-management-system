package com.restaurante.delivery.application;

import com.restaurante.delivery.domain.RutasNoDisponiblesException;
import com.restaurante.delivery.infrastructure.OrsClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Búsqueda de direcciones (geocoding) para el checkout del cliente. Vive en el
 * módulo delivery porque es quien posee {@link OrsClient} y la api-key de ORS;
 * así la clave NO sale de la infraestructura de delivery ni viaja al móvil.
 *
 * <p>El endpoint se publica bajo {@code /api/v1/clientes/...} (igual que la
 * confirmación de recepción) para que la app del cliente lo consuma junto al
 * resto de su flujo. Cualquier indisponibilidad del proveedor se traduce a una
 * lista vacía: la búsqueda nunca rompe el checkout (el cliente ajusta el
 * marcador a mano).
 */
@Service
public class GeocodingService {

    private static final Logger log = LoggerFactory.getLogger(GeocodingService.class);

    private final OrsClient orsClient;

    public GeocodingService(OrsClient orsClient) {
        this.orsClient = orsClient;
    }

    /** Candidato de dirección: etiqueta legible + coordenadas confirmables. */
    public record Candidato(String descripcion, java.math.BigDecimal lat, java.math.BigDecimal lng) {
    }

    /**
     * Busca hasta {@code limite} candidatos para el texto dado. Lista vacía si
     * no hay resultados o si el proveedor no está disponible/configurado.
     */
    public List<Candidato> buscar(String texto, int limite) {
        if (texto == null || texto.isBlank()) {
            return List.of();
        }
        try {
            return orsClient.buscarCandidatos(texto, limite).stream()
                    .map(c -> new Candidato(c.etiqueta(), c.lat(), c.lng()))
                    .toList();
        } catch (RutasNoDisponiblesException e) {
            log.debug("Geocoding no disponible para la búsqueda del cliente: {}", e.getMessage());
            return List.of();
        }
    }
}
