package com.restaurante.delivery;

import com.restaurante.AbstractIntegracionApi;
import com.restaurante.SoporteRegistro;
import com.restaurante.usuarios.EmisorCorreo;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Autorización del módulo delivery (Fase 2): el ADMIN gestiona el estado
 * operativo de los repartidores, un usuario sin el permiso recibe 403, y
 * habilitar a un usuario que no es DELIVERY se rechaza con 422.
 */
class DeliveryAdminAutorizacionIntegrationTest extends AbstractIntegracionApi {

    @MockitoBean
    private EmisorCorreo emisorCorreo;

    @Test
    void adminHabilitaSuspendeYListaRepartidores() throws Exception {
        String admin = tokenAdmin();
        Long usuarioId = registrarRepartidor();

        // Habilitar: nace/queda HABILITADO.
        mockMvc.perform(post("/api/v1/delivery/repartidores/{usuarioId}/habilitar", usuarioId)
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuarioId").value(usuarioId))
                .andExpect(jsonPath("$.estado").value("HABILITADO"))
                .andExpect(jsonPath("$.habilitadoPor").value("admin"));

        // Aparece en el listado de habilitados.
        mockMvc.perform(get("/api/v1/delivery/repartidores/habilitados")
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.usuarioId == " + usuarioId + ")].estado").value(
                        org.hamcrest.Matchers.hasItem("HABILITADO")));

        // Suspender: queda SUSPENDIDO.
        mockMvc.perform(post("/api/v1/delivery/repartidores/{usuarioId}/suspender", usuarioId)
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("SUSPENDIDO"));

        // El listado general lo incluye con su estado.
        mockMvc.perform(get("/api/v1/delivery/repartidores")
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.usuarioId == " + usuarioId + ")].estado").value(
                        org.hamcrest.Matchers.hasItem("SUSPENDIDO")));
    }

    @Test
    void usuarioSinPermisoRecibe403() throws Exception {
        String mesero = tokenMesero();

        mockMvc.perform(get("/api/v1/delivery/repartidores")
                        .header("Authorization", "Bearer " + mesero))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type").value("urn:problem:restaurante:acceso-denegado"));
    }

    @Test
    void habilitarUsuarioNoDeliveryDa422() throws Exception {
        String admin = tokenAdmin();
        Long usuarioMeseroId = crearMesero("mesero_no_delivery_" + Math.abs(System.nanoTime()));

        mockMvc.perform(post("/api/v1/delivery/repartidores/{usuarioId}/habilitar", usuarioMeseroId)
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.type").value("urn:problem:restaurante:regla-de-negocio"));
    }

    /** Registra un repartidor (rol DELIVERY) y devuelve su usuarioId. */
    private Long registrarRepartidor() throws Exception {
        long semilla = System.nanoTime();
        String correo = "repartidor_delivery_" + Math.abs(semilla) + "@test.com";
        String celular = SoporteRegistro.celular(semilla);
        mockMvc.perform(post("/api/v1/auth/registro-repartidor")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","nombre":"Repartidor Fase2","celular":"%s","password":"Clave#123"}
                                """.formatted(correo, celular)))
                .andExpect(status().isCreated());
        return usuarioRepository.findByUsername(correo).orElseThrow().getId();
    }

    /** Crea un usuario MESERO vía panel y devuelve su id. */
    private Long crearMesero(String username) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/usuarios")
                        .header("Authorization", "Bearer " + tokenAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","nombre":"Usuario Prueba","password":"clave123","rolCodigo":"MESERO"}
                                """.formatted(username)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("id").asLong();
    }
}
