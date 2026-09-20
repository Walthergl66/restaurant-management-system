package com.restaurante.mesas;

import com.restaurante.AbstractIntegracionApi;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MesasIntegrationTest extends AbstractIntegracionApi {

    @Test
    void creaYLiberaUnaMesa() throws Exception {
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
                .andExpect(jsonPath("$.ubicacion").value("Terraza"));
    }

    @Test
    void cambiaEstadoYOcupaSoloUnaVez() throws Exception {
        String token = tokenAdmin();
        String id = crearMesa(token, 3);

        mockMvc.perform(patch("/api/v1/mesas/" + id + "/estado")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"estado":"OCUPADA"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("OCUPADA"));

        // Ocupar una mesa ya ocupada viola la regla de negocio (422).
        mockMvc.perform(patch("/api/v1/mesas/" + id + "/estado")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"estado":"OCUPADA"}
                                """))
                .andExpect(status().isUnprocessableEntity());
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