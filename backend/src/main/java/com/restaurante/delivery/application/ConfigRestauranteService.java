package com.restaurante.delivery.application;

import com.restaurante.auditoria.Auditoria;
import com.restaurante.delivery.domain.ConfigRestaurante;
import com.restaurante.delivery.domain.Coordenada;
import com.restaurante.delivery.domain.RutasNoDisponiblesException;
import com.restaurante.delivery.infrastructure.ConfigRestauranteRepository;
import com.restaurante.delivery.infrastructure.OrsClient;
import com.restaurante.shared.domain.exception.BusinessRuleException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Configuración de la ubicación del restaurante: el ORIGEN de las rutas de
 * reparto, PERSISTIDO y editable por el administrador. Reemplaza el origen por
 * env vars ({@code DeliveryOrigenProperties}): no queda ninguna coordenada
 * hardcodeada en el sistema.
 *
 * <p>Geocoding: al cambiar la dirección se intenta resolver con
 * {@link OrsClient#geocodeDireccion(String)}. Si ORS devuelve un resultado claro
 * se fijan coords {@code GEOCODING}; si no devuelve resultado (0 features) o
 * falla, la dirección se guarda SIN coordenadas y queda PENDIENTE de selección
 * manual (no se inventa un punto ni se responde 503 duro): el administrador
 * podrá marcar el punto en el mapa con {@link #fijarCoordenadasManual}.
 */
@Service
@Transactional
public class ConfigRestauranteService {

    private static final String CATEGORIA_AUDITORIA = "DELIVERY";

    private final ConfigRestauranteRepository configs;
    private final OrsClient orsClient;
    private final Auditoria auditoria;
    private final ObjectMapper objectMapper;

    public ConfigRestauranteService(ConfigRestauranteRepository configs, OrsClient orsClient,
                                    Auditoria auditoria, ObjectMapper objectMapper) {
        this.configs = configs;
        this.orsClient = orsClient;
        this.auditoria = auditoria;
        this.objectMapper = objectMapper;
    }

    /** Configuración actual del restaurante (dirección y, si existen, coords). */
    @Transactional(readOnly = true)
    public ConfigRestauranteResumen obtenerConfig() {
        return ConfigRestauranteResumen.de(exigirConfig());
    }

    /**
     * Actualiza la dirección e intenta geocodificarla con ORS. Si ORS resuelve
     * un punto claro, fija coords GEOCODING; si no, deja la config pendiente de
     * selección manual (coords en null). En ambos casos guarda y audita, y el
     * resumen resultante expone {@code requiereSeleccionManual}.
     */
    public ConfigRestauranteResumen actualizarDireccion(String direccion, String actor) {
        ConfigRestaurante config = exigirConfig();
        config.actualizarDireccion(direccion);

        boolean geocodificada = false;
        Optional<Coordenada> resuelto = geocodificarSeguro(config.getDireccion());
        if (resuelto.isPresent()) {
            Coordenada c = resuelto.get();
            config.fijarCoordenadasGeocoding(c.lat(), c.lng());
            geocodificada = true;
        }

        configs.save(config);
        auditar("CONFIG_RESTAURANTE_DIRECCION", Map.of(
                "direccion", config.getDireccion(),
                "geocodificada", String.valueOf(geocodificada),
                "actor", actor != null ? actor : "sistema"));
        return ConfigRestauranteResumen.de(config);
    }

    /**
     * Fija las coordenadas que el administrador marcó en el mapa (origen MANUAL).
     */
    public ConfigRestauranteResumen fijarCoordenadasManual(BigDecimal lat, BigDecimal lng, String actor) {
        if (lat == null || lng == null) {
            throw new BusinessRuleException("La ubicación necesita latitud y longitud");
        }
        ConfigRestaurante config = exigirConfig();
        config.fijarCoordenadasManual(lat, lng);
        configs.save(config);
        auditar("CONFIG_RESTAURANTE_COORDENADAS", Map.of(
                "lat", lat.toPlainString(),
                "lng", lng.toPlainString(),
                "origen", "MANUAL",
                "actor", actor != null ? actor : "sistema"));
        return ConfigRestauranteResumen.de(config);
    }

    /**
     * Origen de las rutas de reparto: la coordenada configurada del restaurante.
     * Fail-closed si no hay coordenadas (igual que antes con env vars): no se
     * inventa una ubicación. Reemplaza a {@code DeliveryOrigenProperties.comoCoordenada()}.
     */
    @Transactional(readOnly = true)
    public Coordenada origenParaRutas() {
        ConfigRestaurante config = configs.findFirstByOrderByIdAsc().orElse(null);
        if (config == null || !config.tieneCoordenadas()) {
            throw new RutasNoDisponiblesException(
                    "El origen de reparto (ubicación del restaurante) no está configurado");
        }
        return new Coordenada(config.getLat(), config.getLng());
    }

    /** True si el restaurante tiene coordenadas de origen configuradas. */
    @Transactional(readOnly = true)
    public boolean origenConfigurado() {
        return configs.findFirstByOrderByIdAsc()
                .map(ConfigRestaurante::tieneCoordenadas)
                .orElse(false);
    }

    private ConfigRestaurante exigirConfig() {
        return configs.findFirstByOrderByIdAsc()
                .orElseThrow(() -> new BusinessRuleException(
                        "No existe la configuración del restaurante"));
    }

    /**
     * Geocodifica sin romper: cualquier indisponibilidad de ORS se trata como
     * "no resuelto" (pendiente de selección manual), no como error duro, para
     * que el administrador siempre pueda guardar la dirección y elegir el punto.
     */
    private Optional<Coordenada> geocodificarSeguro(String direccion) {
        try {
            return orsClient.geocodeDireccion(direccion);
        } catch (RutasNoDisponiblesException e) {
            return Optional.empty();
        }
    }

    private void auditar(String tipo, Map<String, String> detalle) {
        Map<String, String> ordenado = new LinkedHashMap<>(detalle);
        auditoria.registrar(tipo, "CONFIG_RESTAURANTE", "1",
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
