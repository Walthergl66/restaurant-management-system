package com.restaurante.clientes;

import com.restaurante.AbstractIntegracionApi;
import com.restaurante.delivery.infrastructure.OrsClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A1: coordenadas (lat/lng) en las direcciones del cliente y búsqueda de
 * direcciones (geocoding) para el mapa del checkout. ORS simulado con
 * {@link OrsClient} mockeado (sin red ni cuota real).
 */
@DisplayName("Direcciones con coordenadas y geocoding del checkout (A1)")
class ClientesDireccionCoordenadasIntegrationTest extends AbstractIntegracionApi {

    /** ORS simulado: la api-key nunca sale del backend. */
    @MockitoBean
    private OrsClient orsClient;

    private String clienteToken;

    @BeforeEach
    void preparar() {
        Mockito.reset(orsClient);
        clienteToken = tokenCliente("cliente_coords", "claveCoords123");
    }

    @Test
    @DisplayName("crear dirección con lat/lng las persiste y el SPI las devuelve")
    void direccionConCoordenadasPersisteYDevuelve() throws Exception {
        String etiqueta = "Casa-" + System.nanoTime();
        mockMvc.perform(post("/api/v1/clientes/direcciones")
                        .header("Authorization", "Bearer " + clienteToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"etiqueta":"%s","direccion":"Calle 5, casa azul","telefono":"70001122",
                                 "observaciones":"porton negro","lat":13.6929000,"lng":-89.2182000}
                                """.formatted(etiqueta)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lat").value(13.6929))
                .andExpect(jsonPath("$.lng").value(-89.2182));

        // La lista de direcciones también trae las coordenadas confirmadas.
        mockMvc.perform(get("/api/v1/clientes/direcciones")
                        .header("Authorization", "Bearer " + clienteToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.etiqueta=='" + etiqueta + "' && @.lat==13.6929 && @.lng==-89.2182)]")
                        .isNotEmpty());
    }

    @Test
    @DisplayName("crear dirección SIN lat/lng sigue permitido (compatibilidad)")
    void direccionSinCoordenadasSiguePermitida() throws Exception {
        mockMvc.perform(post("/api/v1/clientes/direcciones")
                        .header("Authorization", "Bearer " + clienteToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"etiqueta":"SinCoords-%d","direccion":"Av sin coordenadas","telefono":"70003344"}
                                """.formatted(System.nanoTime())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lat").doesNotExist())
                .andExpect(jsonPath("$.lng").doesNotExist());
    }

    @Test
    @DisplayName("lat fuera de rango -> 400 (validación)")
    void latFueraDeRangoRechazada() throws Exception {
        mockMvc.perform(post("/api/v1/clientes/direcciones")
                        .header("Authorization", "Bearer " + clienteToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"etiqueta":"MalaLat-%d","direccion":"x","lat":200.0,"lng":0.0}
                                """.formatted(System.nanoTime())))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("geocodificar devuelve los candidatos del proveedor (mock)")
    void geocodificarDevuelveCandidatos() throws Exception {
        Mockito.when(orsClient.buscarCandidatos(anyString(), anyInt()))
                .thenReturn(List.of(
                        new OrsClient.CandidatoGeocoding("Centro, San Salvador",
                                new java.math.BigDecimal("13.6929"), new java.math.BigDecimal("-89.2182")),
                        new OrsClient.CandidatoGeocoding("Otra calle",
                                new java.math.BigDecimal("13.7000"), new java.math.BigDecimal("-89.2000"))));

        mockMvc.perform(get("/api/v1/clientes/geocodificar")
                        .header("Authorization", "Bearer " + clienteToken)
                        .param("texto", "centro"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].descripcion").value("Centro, San Salvador"))
                .andExpect(jsonPath("$[0].lat").value(13.6929))
                .andExpect(jsonPath("$[0].lng").value(-89.2182));
    }

    @Test
    @DisplayName("geocodificar sin resultados -> lista vacía, no 500")
    void geocodificarVacioListaVacia() throws Exception {
        Mockito.when(orsClient.buscarCandidatos(anyString(), anyInt())).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/clientes/geocodificar")
                        .header("Authorization", "Bearer " + clienteToken)
                        .param("texto", "direccion inexistente"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("geocodificar con proveedor caído -> lista vacía (no rompe el checkout)")
    void geocodificarProveedorCaidoListaVacia() throws Exception {
        Mockito.when(orsClient.buscarCandidatos(anyString(), anyInt()))
                .thenThrow(new com.restaurante.delivery.domain.RutasNoDisponiblesException("ORS caído"));

        mockMvc.perform(get("/api/v1/clientes/geocodificar")
                        .header("Authorization", "Bearer " + clienteToken)
                        .param("texto", "lo que sea"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
