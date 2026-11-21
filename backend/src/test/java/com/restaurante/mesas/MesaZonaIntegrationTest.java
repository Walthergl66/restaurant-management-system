package com.restaurante.mesas;

import com.restaurante.AbstractIntegracionApi;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import tools.jackson.databind.JsonNode;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Zonas del restaurante: las gestiona el administrador y las mesas se ubican en ellas. */
class MesaZonaIntegrationTest extends AbstractIntegracionApi {

    private static final AtomicInteger NUMERO_MESA = new AtomicInteger(31000);

    private String nombreUnico(String base) {
        return base + " " + (System.nanoTime() % 1_000_000);
    }

    @Test
    void crearUnaZonaDevuelve201ALFinalDelOrden() throws Exception {
        String token = tokenAdmin();
        long antes = listarZonas(token).size();
        String nombre = nombreUnico("terraza");

        mockMvc.perform(post("/api/v1/mesas/zonas")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"%s\"}".formatted(nombre)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value(nombre.substring(0, 1).toUpperCase() + nombre.substring(1)))
                .andExpect(jsonPath("$.orden").value(antes + 1))
                .andExpect(jsonPath("$.cantidadMesas").value(0));
    }

    @Test
    void unNombreRepetidoSinImportarMayusculasEsConflicto() throws Exception {
        String token = tokenAdmin();
        String nombre = nombreUnico("Patio");
        crearZona(token, nombre);

        mockMvc.perform(post("/api/v1/mesas/zonas")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"%s\"}".formatted(nombre.toLowerCase())))
                .andExpect(status().isConflict());
    }

    @Test
    void renombrarUnaZona() throws Exception {
        String token = tokenAdmin();
        long id = crearZona(token, nombreUnico("Jardin"));
        String nuevo = nombreUnico("Jardín interior");

        mockMvc.perform(put("/api/v1/mesas/zonas/" + id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"%s\"}".formatted(nuevo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value(nuevo));
    }

    @Test
    void reordenarConLaListaCompletaCambiaElOrdenYConUnaIncompletaEsRechazado() throws Exception {
        String token = tokenAdmin();
        crearZona(token, nombreUnico("Barra"));
        JsonNode zonas = listarZonas(token);
        StringBuilder invertida = new StringBuilder();
        for (int i = zonas.size() - 1; i >= 0; i--) {
            invertida.append(invertida.length() == 0 ? "" : ",").append(zonas.get(i).path("id").asLong());
        }

        mockMvc.perform(put("/api/v1/mesas/zonas/orden")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ids\":[%s]}".formatted(invertida)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(zonas.get(zonas.size() - 1).path("id").asLong()))
                .andExpect(jsonPath("$[0].orden").value(1));

        mockMvc.perform(put("/api/v1/mesas/zonas/orden")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ids\":[%d]}".formatted(zonas.get(0).path("id").asLong())))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void noSeEliminaUnaZonaConMesasPeroSiUnaVacia() throws Exception {
        String token = tokenAdmin();
        long conMesas = crearZona(token, nombreUnico("ConMesas"));
        crearMesa(token, conMesas);
        long vacia = crearZona(token, nombreUnico("Vacia"));

        mockMvc.perform(delete("/api/v1/mesas/zonas/" + conMesas).header("Authorization", "Bearer " + token))
                .andExpect(status().isUnprocessableEntity());
        mockMvc.perform(delete("/api/v1/mesas/zonas/" + vacia).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    @Test
    void laMesaSeUbicaEnLaZonaElegidaYSinZonaVaALaPrimera() throws Exception {
        String token = tokenAdmin();
        long zona = crearZona(token, nombreUnico("Mirador"));
        long primera = listarZonas(token).get(0).path("id").asLong();

        mockMvc.perform(post("/api/v1/mesas")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"numero\":%d,\"capacidad\":4,\"zonaId\":%d}".formatted(NUMERO_MESA.incrementAndGet(), zona)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.zonaId").value(zona));

        mockMvc.perform(post("/api/v1/mesas")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"numero\":%d,\"capacidad\":2}".formatted(NUMERO_MESA.incrementAndGet())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.zonaId").value(primera));
    }

    @Test
    void laMesaExponeElNombreYElOrdenDeSuZonaYActualizarSinZonaNoLaMueve() throws Exception {
        String token = tokenAdmin();
        String nombre = nombreUnico("Azotea");
        long zona = crearZona(token, nombre);
        long mesaId = crearMesa(token, zona);

        mockMvc.perform(get("/api/v1/mesas/" + mesaId).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ubicacion").value(nombre))
                .andExpect(jsonPath("$.zonaId").value(zona))
                .andExpect(jsonPath("$.zonaOrden").isNumber());

        mockMvc.perform(put("/api/v1/mesas/" + mesaId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"numero\":%d,\"capacidad\":6}".formatted(
                                objectMapper.readTree(mockMvc.perform(get("/api/v1/mesas/" + mesaId)
                                        .header("Authorization", "Bearer " + token)).andReturn().getResponse()
                                        .getContentAsString()).path("numero").asInt())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.zonaId").value(zona));
    }

    @Test
    void listarIncluyeLasZonasVaciasConSuConteo() throws Exception {
        String token = tokenAdmin();
        long vacia = crearZona(token, nombreUnico("SinMesas"));
        JsonNode zonas = listarZonas(token);
        boolean encontrada = false;
        for (JsonNode z : zonas) {
            if (z.path("id").asLong() == vacia) {
                encontrada = true;
                assertEquals(0, z.path("cantidadMesas").asInt());
            }
        }
        assertTrue(encontrada);
    }

    @Test
    void elMeseroPuedeConsultarLasZonasPeroNoGestionarlas() throws Exception {
        String token = tokenMesero();
        mockMvc.perform(get("/api/v1/mesas/zonas").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/mesas/zonas")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"No permitida\"}"))
                .andExpect(status().isForbidden());
    }

    private long crearZona(String token, String nombre) throws Exception {
        String cuerpo = mockMvc.perform(post("/api/v1/mesas/zonas")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"%s\"}".formatted(nombre)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(cuerpo).path("id").asLong();
    }

    private long crearMesa(String token, long zonaId) throws Exception {
        String cuerpo = mockMvc.perform(post("/api/v1/mesas")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"numero\":%d,\"capacidad\":4,\"zonaId\":%d}".formatted(NUMERO_MESA.incrementAndGet(), zonaId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(cuerpo).path("id").asLong();
    }

    private JsonNode listarZonas(String token) throws Exception {
        String cuerpo = mockMvc.perform(get("/api/v1/mesas/zonas").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(cuerpo);
    }
}
