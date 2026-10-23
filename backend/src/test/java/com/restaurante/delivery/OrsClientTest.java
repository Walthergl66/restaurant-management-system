package com.restaurante.delivery;

import com.restaurante.delivery.domain.Coordenada;
import com.restaurante.delivery.domain.CriterioOptimizacion;
import com.restaurante.delivery.domain.ResultadoRuta;
import com.restaurante.delivery.domain.RutasNoDisponiblesException;
import com.restaurante.delivery.infrastructure.OrsClient;
import com.restaurante.delivery.infrastructure.OrsProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import org.springframework.http.HttpMethod;

/**
 * Pruebas del cliente de bajo nivel de ORS con respuestas SIMULADAS
 * ({@link MockRestServiceServer}): validan serialización de la petición y
 * parseo de la respuesta SIN red real ni consumo de cuota. No tocan BD, así que
 * NO necesitan Docker/Testcontainers.
 */
@DisplayName("OrsClient (ORS simulado, sin red)")
class OrsClientTest {

    private static final ObjectMapper MAPPER = JsonMapper.builder().build();

    private OrsProperties conApiKey() {
        OrsProperties p = new OrsProperties();
        p.setApiKey("clave-de-prueba");
        p.setBaseUrl("https://ors.test");
        p.setTimeoutMs(2000);
        return p;
    }

    private record Montaje(OrsClient cliente, MockRestServiceServer server) {
    }

    private Montaje montar(OrsProperties propiedades) {
        RestClient.Builder builder = RestClient.builder().baseUrl(propiedades.getBaseUrl());
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        OrsClient cliente = new OrsClient(builder, propiedades, MAPPER);
        return new Montaje(cliente, server);
    }

    @Test
    @DisplayName("geocode parsea lat/lng del primer feature")
    void geocodeParseaCoordenadas() {
        Montaje m = montar(conApiKey());
        String json = """
                {"features":[{"geometry":{"type":"Point","coordinates":[-89.2182,13.6929]}}]}
                """;
        m.server().expect(requestTo(org.hamcrest.Matchers.containsString("/geocode/search")))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "clave-de-prueba"))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        Optional<Coordenada> c = m.cliente().geocodeDireccion("Av Siempre Viva 123");

        assertTrue(c.isPresent());
        assertEquals(0, new BigDecimal("13.6929").compareTo(c.get().lat()));
        assertEquals(0, new BigDecimal("-89.2182").compareTo(c.get().lng()));
        m.server().verify();
    }

    @Test
    @DisplayName("geocode sin resultados devuelve vacío")
    void geocodeSinResultados() {
        Montaje m = montar(conApiKey());
        m.server().expect(requestTo(org.hamcrest.Matchers.containsString("/geocode/search")))
                .andRespond(withSuccess("{\"features\":[]}", MediaType.APPLICATION_JSON));

        assertTrue(m.cliente().geocodeDireccion("direccion inexistente").isEmpty());
        m.server().verify();
    }

    @Test
    @DisplayName("directions envía coordinates [lng,lat] y parsea distancia/duración/geometría")
    void directionsConstruyeBodyYParsea() {
        Montaje m = montar(conApiKey());
        String json = """
                {"routes":[{"summary":{"distance":1234.5,"duration":600.2},"geometry":"abc_polyline"}]}
                """;
        m.server().expect(requestTo("https://ors.test/v2/directions/driving-car"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "clave-de-prueba"))
                // ORS usa [lng,lat]: origen (lng=-89.2,lat=13.6) primero.
                .andExpect(content().json("""
                        {"coordinates":[[-89.2,13.6],[-89.1,13.7]]}
                        """))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        ResultadoRuta r = m.cliente().calcularRuta(
                List.of(Coordenada.of(13.6, -89.2), Coordenada.of(13.7, -89.1)),
                CriterioOptimizacion.DURACION);

        assertEquals("abc_polyline", r.geometria());
        assertEquals(1235, r.distanciaM());
        assertEquals(600, r.duracionS());
        m.server().verify();
    }

    @Test
    @DisplayName("directions con criterio DISTANCIA pide preference shortest")
    void directionsCriterioDistancia() {
        Montaje m = montar(conApiKey());
        m.server().expect(requestTo("https://ors.test/v2/directions/driving-car"))
                .andExpect(content().json("{\"preference\":\"shortest\"}"))
                .andRespond(withSuccess(
                        "{\"routes\":[{\"summary\":{\"distance\":1,\"duration\":1},\"geometry\":\"g\"}]}",
                        MediaType.APPLICATION_JSON));

        m.cliente().calcularRuta(
                List.of(Coordenada.of(13.6, -89.2), Coordenada.of(13.7, -89.1)),
                CriterioOptimizacion.DISTANCIA);
        m.server().verify();
    }

    @Test
    @DisplayName("optimization devuelve el orden de los jobs")
    void optimizationDevuelveOrden() {
        Montaje m = montar(conApiKey());
        String json = """
                {"routes":[{"steps":[
                  {"type":"start"},
                  {"type":"job","id":2},
                  {"type":"job","id":0},
                  {"type":"job","id":1},
                  {"type":"end"}
                ]}],"unassigned":[]}
                """;
        m.server().expect(requestTo("https://ors.test/optimization"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        List<Integer> orden = m.cliente().optimizarParadas(
                Coordenada.of(13.6, -89.2),
                List.of(Coordenada.of(13.7, -89.1), Coordenada.of(13.8, -89.0), Coordenada.of(13.9, -88.9)),
                CriterioOptimizacion.DURACION);

        assertEquals(List.of(2, 0, 1), orden);
        m.server().verify();
    }

    @Test
    @DisplayName("optimization con unassigned no vacío falla (no inventa orden)")
    void optimizationConUnassignedFalla() {
        Montaje m = montar(conApiKey());
        String json = """
                {"routes":[{"steps":[{"type":"job","id":0}]}],"unassigned":[{"id":1}]}
                """;
        m.server().expect(requestTo("https://ors.test/optimization"))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        assertThrows(RutasNoDisponiblesException.class, () -> m.cliente().optimizarParadas(
                Coordenada.of(13.6, -89.2),
                List.of(Coordenada.of(13.7, -89.1), Coordenada.of(13.8, -89.0)),
                CriterioOptimizacion.DURACION));
        m.server().verify();
    }

    @Test
    @DisplayName("respuesta 5xx se traduce a rutas-no-disponibles")
    void errorServidorSeTraduce() {
        Montaje m = montar(conApiKey());
        m.server().expect(requestTo(org.hamcrest.Matchers.containsString("/geocode/search")))
                .andRespond(withServerError());

        assertThrows(RutasNoDisponiblesException.class,
                () -> m.cliente().geocodeDireccion("algo"));
        m.server().verify();
    }

    @Test
    @DisplayName("4xx se traduce a rutas-no-disponibles")
    void errorClienteSeTraduce() {
        Montaje m = montar(conApiKey());
        m.server().expect(requestTo("https://ors.test/v2/directions/driving-car"))
                .andRespond(withStatus(HttpStatus.FORBIDDEN));

        assertThrows(RutasNoDisponiblesException.class, () -> m.cliente().calcularRuta(
                List.of(Coordenada.of(13.6, -89.2), Coordenada.of(13.7, -89.1)),
                CriterioOptimizacion.DURACION));
        m.server().verify();
    }

    @Test
    @DisplayName("api-key vacía: no llama a ORS y falla fail-closed")
    void apiKeyVaciaNoLlamaAOrs() {
        OrsProperties sinClave = new OrsProperties();
        sinClave.setBaseUrl("https://ors.test");
        Montaje m = montar(sinClave);
        // No se registra ninguna expectativa: si llamara a la red, MockRestServiceServer fallaría.

        assertThrows(RutasNoDisponiblesException.class,
                () -> m.cliente().geocodeDireccion("algo"));
        assertThrows(RutasNoDisponiblesException.class, () -> m.cliente().calcularRuta(
                List.of(Coordenada.of(13.6, -89.2), Coordenada.of(13.7, -89.1)),
                CriterioOptimizacion.DURACION));
        m.server().verify(); // cero peticiones
    }

    private static org.springframework.test.web.client.response.DefaultResponseCreator withStatus(HttpStatus status) {
        return org.springframework.test.web.client.response.MockRestResponseCreators.withStatus(status);
    }
}
