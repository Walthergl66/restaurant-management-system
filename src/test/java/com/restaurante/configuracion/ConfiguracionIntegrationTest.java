package com.restaurante.configuracion;

import com.restaurante.AbstractIntegracionApi;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ConfiguracionIntegrationTest extends AbstractIntegracionApi {

    @Test
    void parametrosPorDefectoDelSeed() throws Exception {
        mockMvc.perform(get("/api/v1/configuracion")
                        .header("Authorization", "Bearer " + tokenAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.clave == 'impuestos.iva')].valor").value("0.15"));
    }

    @Test
    void cambiarNombreDelRestauranteSeReflejaEnElMenu() throws Exception {
        mockMvc.perform(put("/api/v1/configuracion/restaurant.nombre")
                        .header("Authorization", "Bearer " + tokenAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"valor":"Don Gato"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valor").value("Don Gato"));

        mockMvc.perform(get("/api/v1/menu"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.restaurante.nombre").value("Don Gato"));
    }

    @Test
    void meseroNoPuedeCambiarParametros() throws Exception {
        mockMvc.perform(put("/api/v1/configuracion/restaurant.nombre")
                        .header("Authorization", "Bearer " + tokenMesero())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"valor":"Hackeado"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void crudDeImpresoras() throws Exception {
        String token = tokenAdmin();
        mockMvc.perform(post("/api/v1/impresoras")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Cocina","tipo":"TERMICA_RED","ip":"192.168.1.50","puerto":9100,"area":"Cocina"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ip").value("192.168.1.50"))
                .andExpect(jsonPath("$.area").value("Cocina"));
    }
}