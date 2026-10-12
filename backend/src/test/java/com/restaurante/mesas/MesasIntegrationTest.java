package com.restaurante.mesas;

import com.restaurante.AbstractIntegracionApi;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MesasIntegrationTest extends AbstractIntegracionApi {

    @Test
    void creaUnaMesaHabilitada() throws Exception {
        String token = tokenAdmin();
        mockMvc.perform(post("/api/v1/mesas")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"numero":7,"capacidad":4,"ubicacion":"Terraza"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.numero").value(7))
                .andExpect(jsonPath("$.estado").value("LIBRE"))
                .andExpect(jsonPath("$.activo").value(true))
                .andExpect(jsonPath("$.ubicacion").value("Terraza"));
    }

    @Test
    void deshabilitaYVuelveAHabilitarUnaMesa() throws Exception {
        String token = tokenAdmin();
        String id = crearMesa(token, 3);

        mockMvc.perform(patch("/api/v1/mesas/" + id + "/habilitacion")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"habilitada":false}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false))
                .andExpect(jsonPath("$.estado").value("LIBRE"));

        mockMvc.perform(patch("/api/v1/mesas/" + id + "/habilitacion")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"habilitada":true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(true))
                .andExpect(jsonPath("$.estado").value("LIBRE"));
    }

    @Test
    void meseroPuedeVerMesasPeroNoManejarlas() throws Exception {
        String tokenMesero = tokenMesero();
        mockMvc.perform(get("/api/v1/mesas")
                        .header("Authorization", "Bearer " + tokenMesero))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/mesas")
                        .header("Authorization", "Bearer " + tokenMesero)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"numero":99,"capacidad":2}
                                """))
                .andExpect(status().isForbidden());

        String id = crearMesa(tokenAdmin(), 100);
        mockMvc.perform(patch("/api/v1/mesas/" + id + "/habilitacion")
                        .header("Authorization", "Bearer " + tokenMesero)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"habilitada":false}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void eliminaDesactivaLaMesa() throws Exception {
        String token = tokenAdmin();
        String id = crearMesa(token, 12);

        mockMvc.perform(delete("/api/v1/mesas/" + id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/mesas")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == %s)]", id).isEmpty());

        mockMvc.perform(get("/api/v1/mesas?soloActivas=false")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == %s)]", id).isNotEmpty());

        mockMvc.perform(get("/api/v1/mesas/" + id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false));
    }

    @Test
    void rechazaNumeroDuplicadoAlCrearYEditar() throws Exception {
        String token = tokenAdmin();
        String id = crearMesa(token, 21);
        crearMesa(token, 22);

        mockMvc.perform(post("/api/v1/mesas")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"numero":21,"capacidad":4}
                                """))
                .andExpect(status().isConflict());

        mockMvc.perform(put("/api/v1/mesas/" + id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"numero":22,"capacidad":4}
                                """))
                .andExpect(status().isConflict());
    }

    private String crearMesa(String token, int numero) throws Exception {
        var result = mockMvc.perform(post("/api/v1/mesas")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"numero":%d,"capacidad":2}
                                """.formatted(numero)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("id").asText();
    }
}
