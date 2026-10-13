package com.restaurante.catalogo;

import com.restaurante.AbstractIntegracionApi;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CatalogoIntegrationTest extends AbstractIntegracionApi {

    @Nested
    class MenuPublico {

        @Test
        void menuDisponibleSinToken() throws Exception {
            mockMvc.perform(get("/api/v1/menu"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.restaurante.nombre").isNotEmpty())
                    .andExpect(jsonPath("$.restaurante.moneda").value("USD"))
                    .andExpect(jsonPath("$.categorias").isArray());
        }

        @Test
        void menuSoloTraeProductosActivos() throws Exception {
            String token = tokenAdmin();
            MvcResult categoria = mockMvc.perform(post("/api/v1/categorias")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"nombre":"Platos fuertes","orden":1}
                                    """))
                    .andExpect(status().isCreated())
                    .andReturn();
            String categoriaId = objectMapper.readTree(categoria.getResponse().getContentAsString())
                    .path("id").asText();

            MvcResult producto = mockMvc.perform(post("/api/v1/productos")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"nombre":"HamburguesaDoble","descripcion":"Plato del día",
                                     "precio":12.50,"categoriaId":%s,"extraIds":[],"ingredientes":["cebolla"]}
                                    """.formatted(categoriaId)))
                    .andExpect(status().isCreated())
                    .andReturn();
            String productoId = objectMapper.readTree(producto.getResponse().getContentAsString())
                    .path("id").asText();

            MvcResult menu = mockMvc.perform(get("/api/v1/menu"))
                    .andExpect(status().isOk())
                    .andReturn();
            JsonNode platos = platosDe("Platos fuertes", menu);
            assertEquals(1, platos.size());
            assertEquals("HamburguesaDoble", platos.get(0).path("nombre").asText());
            assertEquals(12.50, platos.get(0).path("precio").asDouble());
            assertEquals("Cebolla", platos.get(0).path("ingredientes").get(0).asText());

            mockMvc.perform(delete("/api/v1/productos/" + productoId)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isNoContent());

            MvcResult menuTrasBorrar = mockMvc.perform(get("/api/v1/menu"))
                    .andExpect(status().isOk())
                    .andReturn();
            assertTrue(platosDe("Platos fuertes", menuTrasBorrar).isEmpty());
        }

        /** Productos publicados de una categoría del menú, sin depender del orden
         *  de categorías (la BD Testcontainers es compartida entre clases). */
        private JsonNode platosDe(String nombre, MvcResult menu) throws Exception {
            JsonNode categorias = objectMapper.readTree(menu.getResponse().getContentAsString())
                    .path("categorias");
            for (var categoria : categorias) {
                if (nombre.equals(categoria.path("nombre").asText())) {
                    return categoria.path("productos");
                }
            }
            throw new AssertionError("Categoría '" + nombre + "' no está en el menú");
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
            tools.jackson.databind.JsonNode extras =
                    objectMapper.readTree(extra.getResponse().getContentAsString());
            String extraId = null;
            for (var node : extras) {
                if ("Tocino".equals(node.path("nombre").asText())) {
                    extraId = node.path("id").asText();
                    break;
                }
            }
            assert extraId != null : "Extra 'Tocino' no encontrado";

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
                    .andExpect(jsonPath("$.ingredientes[0]").value("Sal"));
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