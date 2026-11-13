package com.restaurante.delivery;

import com.restaurante.AbstractIntegrationTest;
import com.restaurante.delivery.domain.Coordenada;
import com.restaurante.delivery.domain.CriterioOptimizacion;
import com.restaurante.delivery.domain.ResultadoRuta;
import com.restaurante.delivery.infrastructure.GeocodingCacheRepository;
import com.restaurante.delivery.infrastructure.OrsClient;
import com.restaurante.delivery.infrastructure.OrsProperties;
import com.restaurante.delivery.infrastructure.RutaCacheRepository;
import com.restaurante.delivery.infrastructure.RutasService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Pruebas de la CACHÉ de {@link RutasService} con PostgreSQL real
 * (Testcontainers) y ORS SIMULADO ({@link MockRestServiceServer}). Verifican que
 * una segunda llamada con la misma clave reutiliza la caché y NO vuelve a
 * golpear a ORS (cero consumo de cuota). Estas pruebas SÍ necesitan Docker.
 */
@DisplayName("RutasService caché (Postgres real, ORS simulado)")
class RutasServiceCacheTest extends AbstractIntegrationTest {

    private static final ObjectMapper MAPPER = JsonMapper.builder().build();

    @Autowired
    private GeocodingCacheRepository geocodingCache;
    @Autowired
    private RutaCacheRepository rutaCache;
    @Autowired
    private JdbcTemplate jdbc;

    private RutasService servicio;
    private MockRestServiceServer server;

    @BeforeEach
    void preparar() {
        rutaCache.deleteAll();
        geocodingCache.deleteAll();

        OrsProperties propiedades = new OrsProperties();
        propiedades.setApiKey("clave-de-prueba");
        propiedades.setBaseUrl("https://ors.test");

        RestClient.Builder builder = RestClient.builder().baseUrl(propiedades.getBaseUrl());
        server = MockRestServiceServer.bindTo(builder).build();
        OrsClient cliente = new OrsClient(builder, propiedades, MAPPER);
        servicio = new RutasService(cliente, geocodingCache, rutaCache);
    }

    @Test
    @DisplayName("geocodificar: la 2ª llamada usa caché y NO golpea ORS")
    void geocodificarSegundaLlamadaUsaCache() {
        long direccionId = crearDireccion();
        server.expect(once(), requestTo(org.hamcrest.Matchers.containsString("/geocode/search")))
                .andRespond(withSuccess(
                        "{\"features\":[{\"geometry\":{\"coordinates\":[-89.2,13.6]}}]}",
                        MediaType.APPLICATION_JSON));

        Optional<Coordenada> primera = servicio.geocodificar(direccionId, "Av Siempre Viva 123");
        Optional<Coordenada> segunda = servicio.geocodificar(direccionId, "Av Siempre Viva 123");

        assertTrue(primera.isPresent());
        assertTrue(segunda.isPresent());
        assertEquals(0, primera.get().lat().compareTo(segunda.get().lat()));
        assertEquals(0, new java.math.BigDecimal("13.6").compareTo(segunda.get().lat()));
        // Una sola petición registrada y consumida: la 2ª salió de la caché.
        server.verify();
    }

    /**
     * Crea la cadena mínima usuario -> cliente -> dirección que exige la clave
     * foránea de {@code geocoding_cache.direccion_id} y devuelve el id de la
     * dirección. Usa valores únicos para no chocar con el contenedor compartido.
     */
    private long crearDireccion() {
        String sufijo = Long.toString(System.nanoTime());
        Long usuarioId = jdbc.queryForObject("""
                INSERT INTO seguridad.usuarios (username, password_hash, nombre, rol_id)
                VALUES (?, 'x', 'Cliente Test', (SELECT id FROM seguridad.roles ORDER BY id LIMIT 1))
                RETURNING id
                """, Long.class, "cli-" + sufijo);
        Long clienteId = jdbc.queryForObject("""
                INSERT INTO comercial.clientes (usuario_id, cedula, telefono, nombre)
                VALUES (?, ?, ?, 'Cliente Test')
                RETURNING id
                """, Long.class, usuarioId, "ced" + sufijo, "tel" + sufijo);
        return jdbc.queryForObject("""
                INSERT INTO comercial.clientes_direcciones (cliente_id, etiqueta, direccion)
                VALUES (?, 'Casa', 'Av Siempre Viva 123')
                RETURNING id
                """, Long.class, clienteId);
    }

    @Test
    @DisplayName("rutaPara: la 2ª llamada con la misma clave usa caché y NO golpea ORS")
    void rutaSegundaLlamadaUsaCache() {
        server.expect(once(), requestTo(org.hamcrest.Matchers.containsString("/v2/directions/driving-car/json")))
                .andRespond(withSuccess(
                        "{\"routes\":[{\"summary\":{\"distance\":1500,\"duration\":720},\"geometry\":\"poly\"}]}",
                        MediaType.APPLICATION_JSON));

        List<Coordenada> ruta = List.of(Coordenada.of(13.6, -89.2), Coordenada.of(13.7, -89.1));
        ResultadoRuta primera = servicio.rutaPara(ruta, CriterioOptimizacion.DURACION);
        ResultadoRuta segunda = servicio.rutaPara(ruta, CriterioOptimizacion.DURACION);

        assertEquals("poly", primera.geometria());
        assertEquals(1500, primera.distanciaM());
        assertEquals(primera.geometria(), segunda.geometria());
        assertEquals(primera.distanciaM(), segunda.distanciaM());
        server.verify();
    }
}
