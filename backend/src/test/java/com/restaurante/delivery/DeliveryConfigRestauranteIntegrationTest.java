package com.restaurante.delivery;

import com.restaurante.AbstractIntegracionApi;
import com.restaurante.delivery.application.ConfigRestauranteService;
import com.restaurante.delivery.domain.Coordenada;
import com.restaurante.delivery.domain.RutasNoDisponiblesException;
import com.restaurante.delivery.infrastructure.ConfigRestauranteRepository;
import com.restaurante.delivery.infrastructure.OrsClient;
import com.restaurante.usuarios.EmisorCorreo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Configuración de la ubicación del restaurante (FASE 1): el ORIGEN de las rutas
 * de reparto es ahora una configuración PERSISTIDA que el admin edita, no env
 * vars ni coordenadas hardcodeadas. ORS está MOCKEADO (no se consume cuota).
 *
 * <p>Cubre: (a) geocoding que resuelve -> origen GEOCODING; (b) geocoding
 * vacío/ambiguo -> requiereSeleccionManual sin coords; (c) coords manuales ->
 * origenParaRutas() las devuelve; (d) sin coords -> origenParaRutas() da 503;
 * (e) sin permiso delivery:config-gestionar -> 403.
 */
class DeliveryConfigRestauranteIntegrationTest extends AbstractIntegracionApi {

    @MockitoBean
    private EmisorCorreo emisorCorreo;

    /** ORS simulado: sin red ni cuota real. */
    @MockitoBean
    private OrsClient orsClient;

    @Autowired
    private ConfigRestauranteService configRestaurante;

    @Autowired
    private ConfigRestauranteRepository configs;

    @BeforeEach
    void limpiarCoords() {
        Mockito.reset(orsClient);
        // Deja la config en estado "solo dirección, sin coordenadas" para partir
        // de un punto conocido en cada test (la fila la siembra la migración V14).
        configs.findFirstByOrderByIdAsc().ifPresent(c -> {
            c.actualizarDireccion("Direccion base distinta " + System.nanoTime());
            c.actualizarDireccion("5 de Junio, Jaramijo, Manabi, Ecuador");
            configs.save(c);
        });
    }

    @Test
    void geocodingResueltoDejaOrigenGeocoding() throws Exception {
        String admin = tokenAdmin();
        Mockito.when(orsClient.geocodeDireccion(anyString()))
                .thenReturn(Optional.of(Coordenada.of(-0.9576, -80.5920)));

        mockMvc.perform(put("/api/v1/delivery/config-restaurante/direccion")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"direccion":"5 de Junio, Jaramijo, Manabi, Ecuador"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.direccion").value("5 de Junio, Jaramijo, Manabi, Ecuador"))
                .andExpect(jsonPath("$.origenCoords").value("GEOCODING"))
                .andExpect(jsonPath("$.tieneCoordenadas").value(true))
                .andExpect(jsonPath("$.requiereSeleccionManual").value(false));

        Coordenada origen = configRestaurante.origenParaRutas();
        assertThat(origen.lat()).isEqualByComparingTo("-0.9576");
        assertThat(origen.lng()).isEqualByComparingTo("-80.5920");
    }

    @Test
    void geocodingVacioRequiereSeleccionManual() throws Exception {
        String admin = tokenAdmin();
        Mockito.when(orsClient.geocodeDireccion(anyString()))
                .thenReturn(Optional.empty());

        mockMvc.perform(put("/api/v1/delivery/config-restaurante/direccion")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"direccion":"Direccion ambigua sin resultado"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.origenCoords").doesNotExist())
                .andExpect(jsonPath("$.tieneCoordenadas").value(false))
                .andExpect(jsonPath("$.requiereSeleccionManual").value(true));
    }

    @Test
    void coordenadasManualesSeUsanComoOrigen() throws Exception {
        String admin = tokenAdmin();

        mockMvc.perform(put("/api/v1/delivery/config-restaurante/coordenadas")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"lat":-0.9570000,"lng":-80.5910000}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.origenCoords").value("MANUAL"))
                .andExpect(jsonPath("$.tieneCoordenadas").value(true))
                .andExpect(jsonPath("$.requiereSeleccionManual").value(false));

        Coordenada origen = configRestaurante.origenParaRutas();
        assertThat(origen.lat()).isEqualByComparingTo("-0.957");
        assertThat(origen.lng()).isEqualByComparingTo("-80.591");
    }

    @Test
    void sinCoordenadasOrigenParaRutasDa503() {
        // Tras limpiarCoords la config no tiene coordenadas.
        assertThatThrownBy(() -> configRestaurante.origenParaRutas())
                .isInstanceOf(RutasNoDisponiblesException.class);
    }

    @Test
    void sinPermisoConfigGestionarDa403() throws Exception {
        String mesero = tokenMesero();

        mockMvc.perform(get("/api/v1/delivery/config-restaurante")
                        .header("Authorization", "Bearer " + mesero))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type").value("urn:problem:restaurante:acceso-denegado"));
    }
}
