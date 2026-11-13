package com.restaurante.delivery;

import com.restaurante.delivery.infrastructure.OrsClient;
import com.restaurante.delivery.infrastructure.OrsProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Pruebas del buscador de candidatos de geocoding ({@code buscarCandidatos})
 * con respuestas SIMULADAS ({@link MockRestServiceServer}): sin red real ni
 * consumo de cuota. No tocan BD, así que no necesitan Docker/Testcontainers.
 */
@DisplayName("OrsClient.buscarCandidatos (ORS simulado, sin red)")
class OrsClientGeocodingBuscarTest {

    private static final ObjectMapper MAPPER = JsonMapper.builder().build();

    private record Montaje(OrsClient cliente, MockRestServiceServer server) {
    }

    private Montaje montar() {
        OrsProperties p = new OrsProperties();
        p.setApiKey("clave-de-prueba");
        p.setBaseUrl("https://ors.test");
        p.setTimeoutMs(2000);
        RestClient.Builder builder = RestClient.builder().baseUrl(p.getBaseUrl());
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        return new Montaje(new OrsClient(builder, p, MAPPER), server);
    }

    @Test
    @DisplayName("parsea varios candidatos con etiqueta y coordenadas (lat,lng)")
    void parseaVariosCandidatos() {
        Montaje m = montar();
        String json = """
                {"features":[
                  {"geometry":{"coordinates":[-89.2182,13.6929]},"properties":{"label":"Centro, San Salvador"}},
                  {"geometry":{"coordinates":[-89.2000,13.7000]},"properties":{"label":"Otra calle"}}
                ]}
                """;
        m.server().expect(requestTo(org.hamcrest.Matchers.containsString("/geocode/search")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        List<OrsClient.CandidatoGeocoding> c = m.cliente().buscarCandidatos("centro", 5);

        assertEquals(2, c.size());
        assertEquals("Centro, San Salvador", c.get(0).etiqueta());
        assertEquals(0, new BigDecimal("13.6929").compareTo(c.get(0).lat()));
        assertEquals(0, new BigDecimal("-89.2182").compareTo(c.get(0).lng()));
        m.server().verify();
    }

    @Test
    @DisplayName("sin resultados devuelve lista vacía (no lanza)")
    void sinResultadosListaVacia() {
        Montaje m = montar();
        m.server().expect(requestTo(org.hamcrest.Matchers.containsString("/geocode/search")))
                .andRespond(withSuccess("{\"features\":[]}", MediaType.APPLICATION_JSON));

        assertTrue(m.cliente().buscarCandidatos("nada", 5).isEmpty());
        m.server().verify();
    }

    @Test
    @DisplayName("texto en blanco no llama a ORS y devuelve vacío")
    void textoEnBlancoNoLlama() {
        Montaje m = montar();
        // Sin expectativas: si llamara a la red, el mock fallaría.
        assertTrue(m.cliente().buscarCandidatos("   ", 5).isEmpty());
        m.server().verify();
    }
}
