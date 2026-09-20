package com.restaurante.catalogo;

import com.restaurante.AbstractIntegracionApi;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CatalogoIntegrationTest extends AbstractIntegracionApi {

    private String crearProducto(String nombre, String token) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/productos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"%s","descripcion":"Plato del día","precio":12.50,
                                 "extraIds":[],"ingredientes":["cebolla","tomate"]}
                                """.formatted(nombre)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("id").asText();
    }

    @Nested
    class MenuPublico {

        @Test
        void menuDisponibleSinToken() throws Exception {
            mockMvc.perform(get("/api/v1/menu"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.restaurante.nombre").value("Restaurante"))
                    .andExpect(jsonPath("$.restaurante.moneda").value("USD"))
                    .andExpect(jsonPath("$.categorias").isArray());
        }

        @Test
        void menuSoloTraeProductosActivos() throws Exception {
            String token = tokenAdmin();
            mockMvc.perform(post("/api/v1/categorias")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"nombre":"Platos fuertes","orden":1}
                                    """))
                    .andExpect(status().isCreated());

            String id = crearProducto("HamburguesaDoble", token);

            mockMvc.perform(get("/api/v1/menu"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.categorias[0].nombre").value("Platos fuertes"))
                    .andExpect(jsonPath("$.categorias[0].productos[0].nombre").value("HamburguesaDoble"))
                    .andExpect(jsonPath("$.categorias[0].productos[0].precio").value(12.50))
                    .andExpect(jsonPath("$.categorias[0].productos[0].ingredientes[0]").value("cebolla"));

            mockMvc.perform(delete("/api/v1/productos/" + id)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isNoContent());

            mockMvc.perform(get("/api/v1/menu"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.categorias[0].productos").isEmpty());
        }
    }

    @Nested
    class Crud {

        @Test
        void creaCategoriaAreaYExtra() throws Exception {
            String token = tokenAdmin();
            mockMvc.perform(post("/api/v1/categorias")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"nombre":"Postres","orden":9}
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.nombre").value("Postres"));

            mockMvc.perform(post("/api/v1/areas")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"nombre":"Cocina","descripcion":"Platos calientes"}
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.nombre").value("Cocina"));

            mockMvc.perform(post("/api/v1/extras")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"nombre":"Queso extra","precio":1.50}
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.precio").value(1.50));
        }

        @Test
        void productoConExtrasEIngredientes() throws Exception {
            String token = tokenAdmin();
            mockMvc.perform(post("/api/v1/extras")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"nombre":"Tocino","precio":2.00}
                                    """))
                    .andExpect(status().isCreated());

            MvcResult extra = mockMvc.perform(get("/api/v1/extras")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andReturn();
            String extraId = objectMapper.readTree(extra.getResponse().getContentAsString())
                    .path(0).path("id").asText();

            mockMvc.perform(post("/api/v1/productos")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"nombre":"Papas Super","precio":6.00,
                                     "extraIds":[%s],"ingredientes":["sal"]}
                                    """.formatted(extraId)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.precio").value(6.00))
                    .andExpect(jsonPath("$.extras[0].nombre").value("Tocino"))
                    .andExpect(jsonPath("$.ingredientes[0]").value("sal"));
        }

        @Test
        void meseroNoPuedeCrearProducto() throws Exception {
            String tokenMesero = tokenMesero();
            mockMvc.perform(post("/api/v1/productos")
                            .header("Authorization", "Bearer " + tokenMesero)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"nombre":"Clandestino","precio":1.00,"extraIds":[],"ingredientes":[]}
                                    """))
                    .andExpect(status().isForbidden());
        }
    }
}