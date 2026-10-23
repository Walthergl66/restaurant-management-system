package com.restaurante.delivery.infrastructure;

import com.restaurante.delivery.domain.Coordenada;
import com.restaurante.delivery.domain.CriterioOptimizacion;
import com.restaurante.delivery.domain.ResultadoRuta;
import com.restaurante.delivery.domain.RutasNoDisponiblesException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Cliente de bajo nivel de OpenRouteService: encapsula EXCLUSIVAMENTE la
 * comunicación HTTP con los tres servicios del plan gratuito (Geocoding,
 * Directions y Optimization/VROOM). No añade caché ni lógica de negocio; eso
 * vive en {@link com.restaurante.delivery.infrastructure.RutasService}.
 *
 * <p>Control de errores uniforme: sin api-key no se llama a ORS (fail-closed);
 * cualquier timeout, respuesta 4xx/5xx o cuerpo inesperado se traduce a
 * {@link RutasNoDisponiblesException}. La api-key NUNCA aparece en los mensajes
 * ni en los logs. Se hace un único intento por petición (sin reintentos) para
 * no quemar la cuota gratuita.
 */
@Component
public class OrsClient {

    private static final Logger log = LoggerFactory.getLogger(OrsClient.class);

    /** Perfil de vehículo del plan gratuito usado en Directions y Optimization. */
    private static final String PERFIL = "driving-car";

    private final RestClient restClient;
    private final OrsProperties propiedades;
    private final ObjectMapper objectMapper;

    public OrsClient(@Qualifier(OrsConfig.ORS_REST_CLIENT_BUILDER) RestClient.Builder builder,
                     OrsProperties propiedades, ObjectMapper objectMapper) {
        this.restClient = builder.build();
        this.propiedades = propiedades;
        this.objectMapper = objectMapper;
    }

    /**
     * Geocodifica un texto de dirección con ORS Geocoding (Pelias):
     * {@code GET /geocode/search?text=...&size=1}. Devuelve las coordenadas del
     * primer resultado, o vacío si ORS no encontró ninguno.
     */
    public Optional<Coordenada> geocodeDireccion(String textoDireccion) {
        exigirApiKey();
        if (textoDireccion == null || textoDireccion.isBlank()) {
            return Optional.empty();
        }
        JsonNode cuerpo = ejecutar("geocoding", () -> restClient.get()
                .uri(uri -> uri.path("/geocode/search")
                        .queryParam("text", textoDireccion)
                        .queryParam("size", 1)
                        .build())
                .header("Authorization", propiedades.getApiKey())
                .header("Accept", "application/json")
                .retrieve()
                .body(JsonNode.class));

        JsonNode features = cuerpo == null ? null : cuerpo.get("features");
        if (features == null || !features.isArray() || features.isEmpty()) {
            return Optional.empty();
        }
        JsonNode coords = features.get(0).path("geometry").path("coordinates");
        if (!coords.isArray() || coords.size() < 2) {
            throw new RutasNoDisponiblesException("Respuesta de geocoding sin coordenadas válidas");
        }
        // ORS entrega [lng, lat]; se guarda como (lat, lng).
        BigDecimal lng = coords.get(0).decimalValue();
        BigDecimal lat = coords.get(1).decimalValue();
        return Optional.of(new Coordenada(lat, lng));
    }

    /**
     * Candidato de búsqueda de ORS Geocoding: etiqueta legible + coordenadas.
     */
    public record CandidatoGeocoding(String etiqueta, BigDecimal lat, BigDecimal lng) {
    }

    /**
     * Busca varios candidatos de dirección con ORS Geocoding (Pelias):
     * {@code GET /geocode/search?text=...&size=limite}. Devuelve de 0 a
     * {@code limite} candidatos con su etiqueta legible ({@code label}) y
     * coordenadas. Nunca lanza por "sin resultados": devuelve lista vacía.
     */
    public List<CandidatoGeocoding> buscarCandidatos(String textoDireccion, int limite) {
        exigirApiKey();
        if (textoDireccion == null || textoDireccion.isBlank()) {
            return List.of();
        }
        int size = Math.max(1, Math.min(limite, 10));
        JsonNode cuerpo = ejecutar("geocoding", () -> restClient.get()
                .uri(uri -> uri.path("/geocode/search")
                        .queryParam("text", textoDireccion)
                        .queryParam("size", size)
                        .build())
                .header("Authorization", propiedades.getApiKey())
                .header("Accept", "application/json")
                .retrieve()
                .body(JsonNode.class));

        JsonNode features = cuerpo == null ? null : cuerpo.get("features");
        if (features == null || !features.isArray() || features.isEmpty()) {
            return List.of();
        }
        List<CandidatoGeocoding> candidatos = new ArrayList<>();
        for (JsonNode feature : features) {
            JsonNode coords = feature.path("geometry").path("coordinates");
            if (!coords.isArray() || coords.size() < 2) {
                continue;
            }
            // ORS entrega [lng, lat]; se expone como (lat, lng).
            BigDecimal lng = coords.get(0).decimalValue();
            BigDecimal lat = coords.get(1).decimalValue();
            String etiqueta = feature.path("properties").path("label").asString(textoDireccion);
            candidatos.add(new CandidatoGeocoding(etiqueta, lat, lng));
        }
        return candidatos;
    }

    /**
     * Calcula una ruta que pasa por las coordenadas en el orden dado con ORS
     * Directions: {@code POST /v2/directions/driving-car}. El cuerpo envía
     * {@code coordinates} como {@code [[lng,lat],...]} (orden de ORS).
     *
     * <p>El criterio {@code DISTANCIA} se traduce a {@code preference:shortest}
     * (ruta más corta); {@code DURACION} usa el valor por defecto de ORS
     * (más rápida). ORS NO garantiza optimalidad matemática del orden de
     * paradas: eso lo decide {@link #optimizarParadas}.
     */
    public ResultadoRuta calcularRuta(List<Coordenada> enOrden, CriterioOptimizacion criterio) {
        exigirApiKey();
        if (enOrden == null || enOrden.size() < 2) {
            throw new RutasNoDisponiblesException("Una ruta necesita al menos origen y destino");
        }

        ObjectNode peticion = objectMapper.createObjectNode();
        ArrayNode coordinates = peticion.putArray("coordinates");
        for (Coordenada c : enOrden) {
            ArrayNode par = coordinates.addArray();
            par.add(c.lng());
            par.add(c.lat());
        }
        if (criterio == CriterioOptimizacion.DISTANCIA) {
            peticion.put("preference", "shortest");
        }

        JsonNode cuerpo = ejecutar("directions", () -> restClient.post()
                .uri("/v2/directions/" + PERFIL)
                .header("Authorization", propiedades.getApiKey())
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .body(peticion)
                .retrieve()
                .body(JsonNode.class));

        JsonNode rutas = cuerpo == null ? null : cuerpo.get("routes");
        if (rutas == null || !rutas.isArray() || rutas.isEmpty()) {
            throw new RutasNoDisponiblesException("Respuesta de directions sin rutas");
        }
        JsonNode ruta = rutas.get(0);
        JsonNode resumen = ruta.path("summary");
        String geometria = ruta.path("geometry").asString("");
        if (geometria.isBlank()) {
            throw new RutasNoDisponiblesException("Respuesta de directions sin geometría");
        }
        int distanciaM = (int) Math.round(resumen.path("distance").asDouble(0));
        int duracionS = (int) Math.round(resumen.path("duration").asDouble(0));
        return new ResultadoRuta(geometria, distanciaM, duracionS);
    }

    /**
     * Optimiza el orden de visita de los destinos desde un origen con ORS
     * Optimization (VROOM): {@code POST /optimization}, un vehículo que parte y
     * termina en el origen y un job por destino. Devuelve los índices de los
     * destinos (0-based, en el orden de la lista {@code destinos}) en el orden
     * eficiente de visita.
     *
     * <p>Si ORS no puede asignar todos los jobs ({@code unassigned} no vacío) se
     * lanza {@link RutasNoDisponiblesException}: NO se inventa un orden parcial.
     */
    public List<Integer> optimizarParadas(Coordenada origen, List<Coordenada> destinos,
                                          CriterioOptimizacion criterio) {
        exigirApiKey();
        if (origen == null || destinos == null || destinos.isEmpty()) {
            throw new RutasNoDisponiblesException("La optimización necesita origen y al menos un destino");
        }

        ObjectNode peticion = objectMapper.createObjectNode();

        ArrayNode vehiculos = peticion.putArray("vehicles");
        ObjectNode vehiculo = vehiculos.addObject();
        vehiculo.put("id", 1);
        vehiculo.put("profile", PERFIL);
        agregarCoordenada(vehiculo.putArray("start"), origen);
        agregarCoordenada(vehiculo.putArray("end"), origen);

        ArrayNode jobs = peticion.putArray("jobs");
        for (int i = 0; i < destinos.size(); i++) {
            ObjectNode job = jobs.addObject();
            // id = índice (base 0) del destino en la lista de entrada.
            job.put("id", i);
            agregarCoordenada(job.putArray("location"), destinos.get(i));
        }
        // VROOM optimiza por duración por defecto; el criterio se documenta pero
        // ORS Optimization no expone 'shortest' a este nivel.

        JsonNode cuerpo = ejecutar("optimization", () -> restClient.post()
                .uri("/optimization")
                .header("Authorization", propiedades.getApiKey())
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .body(peticion)
                .retrieve()
                .body(JsonNode.class));

        JsonNode unassigned = cuerpo == null ? null : cuerpo.get("unassigned");
        if (unassigned != null && unassigned.isArray() && !unassigned.isEmpty()) {
            throw new RutasNoDisponiblesException(
                    "ORS no pudo asignar todas las paradas (" + unassigned.size() + " sin ruta)");
        }

        JsonNode rutas = cuerpo == null ? null : cuerpo.get("routes");
        if (rutas == null || !rutas.isArray() || rutas.isEmpty()) {
            throw new RutasNoDisponiblesException("Respuesta de optimization sin rutas");
        }

        List<Integer> orden = new ArrayList<>();
        for (JsonNode paso : rutas.get(0).path("steps")) {
            if ("job".equals(paso.path("type").asString(""))) {
                orden.add(paso.path("id").asInt());
            }
        }
        if (orden.isEmpty()) {
            throw new RutasNoDisponiblesException("Respuesta de optimization sin paradas resueltas");
        }
        return orden;
    }

    private void exigirApiKey() {
        if (!propiedades.tieneApiKey()) {
            // Fail-closed en dev: sin clave no se llama a ORS ni se inventa ruta.
            throw new RutasNoDisponiblesException("El servicio de rutas no está configurado");
        }
    }

    private static void agregarCoordenada(ArrayNode destino, Coordenada c) {
        destino.add(c.lng());
        destino.add(c.lat());
    }

    /**
     * Ejecuta una llamada a ORS traduciendo cualquier fallo de red o respuesta a
     * {@link RutasNoDisponiblesException}, sin filtrar la api-key. Un único
     * intento (sin reintentos) para no gastar la cuota.
     */
    private JsonNode ejecutar(String operacion, java.util.function.Supplier<JsonNode> llamada) {
        try {
            return llamada.get();
        } catch (RestClientException e) {
            // e.getMessage() de RestClient no incluye cabeceras, pero por si
            // acaso no se concatena nada derivado de la api-key.
            log.warn("ORS {} no disponible: {}", operacion, e.getClass().getSimpleName());
            throw new RutasNoDisponiblesException("El servicio de rutas no está disponible (" + operacion + ")", e);
        }
    }
}
