package com.restaurante.usuarios;

import com.restaurante.AbstractIntegracionApi;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Taxonomía de roles (categorías y banderas) y la regla de que el panel solo
 * asigna los roles marcados como asignables: ADMIN es una cuenta única y
 * CLIENTE y DELIVERY nacen por auto-registro.
 */
class TaxonomiaRolesIntegrationTest extends AbstractIntegracionApi {

    @Test
    void laTaxonomiaAgrupaLosRolesPorCategoria() throws Exception {
        mockMvc.perform(get("/api/v1/usuarios/roles")
                        .header("Authorization", "Bearer " + tokenAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categorias.length()").value(4))
                .andExpect(jsonPath("$.categorias[0].codigo").value("ADMIN"))
                .andExpect(jsonPath("$.categorias[1].codigo").value("CLIENTE"))
                .andExpect(jsonPath("$.categorias[2].codigo").value("DELIVERY"))
                .andExpect(jsonPath("$.categorias[3].codigo").value("EMPLEADO"))
                .andExpect(jsonPath("$.categorias[3].roles.length()").value(2))
                .andExpect(jsonPath("$.categorias[3].roles[0].codigo").value("CAJERO"))
                .andExpect(jsonPath("$.categorias[3].roles[1].codigo").value("MESERO"))
                .andExpect(jsonPath("$.roles.length()").value(5));
    }

    @Test
    void cadaRolExponeSuCategoriaYSusBanderas() throws Exception {
        mockMvc.perform(get("/api/v1/usuarios/roles")
                        .header("Authorization", "Bearer " + tokenAdmin()))
                .andExpect(status().isOk())
                // ADMIN: una sola cuenta, ni asignable ni configurable.
                .andExpect(jsonPath("$.categorias[0].roles[0].asignablePorAdmin").value(false))
                .andExpect(jsonPath("$.categorias[0].roles[0].configurable").value(false))
                .andExpect(jsonPath("$.categorias[0].roles[0].origen").value("ADMIN"))
                // CLIENTE y DELIVERY: nacen por auto-registro.
                .andExpect(jsonPath("$.categorias[1].roles[0].asignablePorAdmin").value(false))
                .andExpect(jsonPath("$.categorias[1].roles[0].configurable").value(false))
                .andExpect(jsonPath("$.categorias[1].roles[0].origen").value("AUTO_REGISTRO"))
                .andExpect(jsonPath("$.categorias[2].roles[0].asignablePorAdmin").value(false))
                .andExpect(jsonPath("$.categorias[2].roles[0].configurable").value(false))
                .andExpect(jsonPath("$.categorias[2].roles[0].origen").value("AUTO_REGISTRO"))
                // MESERO y CAJERO: el panel los asigna y les ajusta funcionalidades.
                .andExpect(jsonPath("$.categorias[3].roles[0].categoria").value("EMPLEADO"))
                .andExpect(jsonPath("$.categorias[3].roles[0].categoriaDescripcion").isNotEmpty())
                .andExpect(jsonPath("$.categorias[3].roles[0].asignablePorAdmin").value(true))
                .andExpect(jsonPath("$.categorias[3].roles[0].configurable").value(true))
                .andExpect(jsonPath("$.categorias[3].roles[1].asignablePorAdmin").value(true))
                .andExpect(jsonPath("$.categorias[3].roles[1].configurable").value(true));
    }

    @Test
    void elPanelRechazaLosRolesNoAsignables() throws Exception {
        for (String rol : new String[]{"ADMIN", "CLIENTE", "DELIVERY"}) {
            mockMvc.perform(post("/api/v1/usuarios")
                            .header("Authorization", "Bearer " + tokenAdmin())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"username":"no_asignable_%s","nombre":"Usuario Prueba","password":"clave123","rolCodigo":"%s"}
                                    """.formatted(rol.toLowerCase(), rol)))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.type").value("urn:problem:restaurante:regla-de-negocio"))
                    .andExpect(jsonPath("$.detail", containsString("no se asigna desde el panel")));
        }
    }

    @Test
    void elPanelSiAsignaLosRolesAsignables() throws Exception {
        mockMvc.perform(post("/api/v1/usuarios")
                        .header("Authorization", "Bearer " + tokenAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"mesero_asignable_%s","nombre":"Usuario Prueba","password":"clave123","rolCodigo":"MESERO"}
                                """.formatted(System.nanoTime() % 1_000_000_000L)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rol").value("MESERO"));
    }

    @Test
    void cambiarARolNoAsignableSeRechaza() throws Exception {
        Long id = crearMesero("cambio_rol_" + System.nanoTime() % 1_000_000_000L);

        mockMvc.perform(put("/api/v1/usuarios/{id}", id)
                        .header("Authorization", "Bearer " + tokenAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Usuario Prueba","rolCodigo":"DELIVERY"}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail", containsString("no se asigna desde el panel")));

        // El usuario sigue con su rol: el cambio rechazado no se aplica.
        mockMvc.perform(get("/api/v1/usuarios/{id}", id)
                        .header("Authorization", "Bearer " + tokenAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol").value("MESERO"));
    }

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
