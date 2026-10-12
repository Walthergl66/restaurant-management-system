package com.restaurante.catalogo;

import com.restaurante.AbstractIntegracionApi;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;

import java.util.Locale;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CatalogoHabilitacionIntegrationTest extends AbstractIntegracionApi {
    @ParameterizedTest
    @ValueSource(strings = {"productos", "categorias", "areas", "extras"})
    void habilitacionConservaIdYNombreReservadoInclusoInactivo(String recurso) throws Exception {
        String token = tokenAdmin();
        String nombre = "Registro-" + UUID.randomUUID();
        long id = crear(recurso, token, nombre);
        long otroId = crear(recurso, token, "Otro-" + UUID.randomUUID());
        String ruta = "/api/v1/" + recurso;

        mockMvc.perform(patch(ruta + "/" + id + "/habilitacion")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"habilitada\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.activo").value(false));

        String variante = "  " + nombre.toUpperCase(Locale.ROOT) + "  ";
        mockMvc.perform(post(ruta).header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(payload(recurso, variante)))
                .andExpect(status().isConflict());
        mockMvc.perform(put(ruta + "/" + otroId).header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(payload(recurso, variante)))
                .andExpect(status().isConflict());

        // Cambiar la escritura del propio nombre no crea una segunda identidad.
        mockMvc.perform(put(ruta + "/" + id).header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(payload(recurso, variante)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.activo").value(false));
        for (int i = 0; i < 2; i++) {
            mockMvc.perform(patch(ruta + "/" + id + "/habilitacion")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON).content("{\"habilitada\":true}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(id))
                    .andExpect(jsonPath("$.activo").value(true));
        }
        mockMvc.perform(get(ruta + "/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.activo").value(true));
    }

    @ParameterizedTest
    @ValueSource(strings = {"productos", "categorias", "areas", "extras"})
    void validaHabilitacionYPermisos(String recurso) throws Exception {
        String admin = tokenAdmin();
        long id = crear(recurso, admin, "Permisos-" + UUID.randomUUID());
        String ruta = "/api/v1/" + recurso + "/" + id + "/habilitacion";
        for (String cuerpo : new String[] {"{}", "{\"habilitada\":null}"}) {
            mockMvc.perform(patch(ruta).header("Authorization", "Bearer " + admin)
                            .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                    .andExpect(status().isBadRequest());
        }
        mockMvc.perform(patch(ruta).header("Authorization", "Bearer " + tokenMesero())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"habilitada\":false}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/" + recurso + "/" + id).header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.activo").value(true));
        mockMvc.perform(patch("/api/v1/" + recurso + "/9223372036854775807/habilitacion")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"habilitada\":true}"))
                .andExpect(status().isNotFound());
    }

    private long crear(String recurso, String token, String nombre) throws Exception {
        var result = mockMvc.perform(post("/api/v1/" + recurso)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(payload(recurso, nombre)))
                .andExpect(status().isCreated()).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("id").asLong();
    }

    private String payload(String recurso, String nombre) {
        String campos = switch (recurso) {
            case "productos" -> ",\"precio\":1.00,\"extraIds\":[],\"ingredientes\":[]";
            case "categorias" -> ",\"orden\":1";
            case "extras" -> ",\"precio\":1.00";
            default -> "";
        };
        return "{\"nombre\":\"" + nombre + "\"" + campos + "}";
    }
}
