package com.restaurante.archivos;

import com.restaurante.AbstractIntegracionApi;
import com.restaurante.archivos.application.*;
import com.restaurante.archivos.infrastructure.ArchivoRepository;
import com.restaurante.shared.domain.exception.AlmacenamientoNoDisponible;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import java.awt.image.BufferedImage;
import java.io.*;

import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import javax.imageio.ImageIO;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ArchivosIntegrationTest extends AbstractIntegracionApi {
    private static final Map<String, byte[]> DISCO=new ConcurrentHashMap<>();
    private static final ServidorArchivosPrueba NGINX;
    static {
        try { NGINX=new ServidorArchivosPrueba(DISCO); }
        catch(IOException ex) { throw new ExceptionInInitializerError(ex); }
    }
    @AfterAll static void pararHttp() throws IOException { NGINX.close(); }
    @DynamicPropertySource static void config(DynamicPropertyRegistry registry) {
        registry.add("app.archivos.habilitado", () -> true);
        registry.add("app.archivos.clave-privada", () -> "solo-test");
        registry.add("app.archivos.known-hosts", () -> "solo-test");
        registry.add("app.archivos.lectura-url", () -> "http://127.0.0.1:"+NGINX.getPort()+"/media/");
        registry.add("app.archivos.limpieza-intervalo-ms", () -> 3_600_000);
    }
    @MockitoBean AlmacenArchivos almacen;
    @Autowired ArchivoRepository archivos;
    @Autowired LimpiezaArchivos limpieza;
    @Autowired JdbcTemplate jdbc;
    @BeforeEach void almacenamiento() {
        doAnswer(inv -> { DISCO.put(inv.getArgument(1), Files.readAllBytes(inv.getArgument(0, Path.class))); return null; }).when(almacen).guardar(any(), anyString());
        doAnswer(inv -> { DISCO.remove(inv.getArgument(0, String.class)); return null; }).when(almacen).eliminar(anyString());
    }
    private byte[] png() throws IOException {
        var out=new ByteArrayOutputStream(); ImageIO.write(new BufferedImage(8,8,BufferedImage.TYPE_INT_RGB), "png", out); return out.toByteArray();
    }
    private tools.jackson.databind.JsonNode subir(String token) throws Exception {
        var response=mockMvc.perform(multipart("/api/v1/archivos").file(new MockMultipartFile("archivo","foto.png","image/png",png())).header("Authorization","Bearer "+token))
                .andExpect(status().isCreated()).andReturn().getResponse();
        return objectMapper.readTree(response.getContentAsString());
    }
    private Map<String,Object> producto(String nombre, tools.jackson.databind.JsonNode archivo) {
        return new HashMap<>(Map.of("nombre",nombre,"precio",1.50,"extraIds",List.of(),"ingredientes",List.of(),
                "imagenUrl",archivo.path("url").asText(),"imagenArchivoId",archivo.path("id").asText()));
    }
    @Test void subidaVinculacionLecturaPersistenciaYReemplazo() throws Exception {
        String token=tokenAdmin(); var foto=subir(token); String url=foto.path("url").asText();
        mockMvc.perform(get(url)).andExpect(status().isNotFound());
        mockMvc.perform(get(url).header("Authorization","Bearer "+token)).andExpect(status().isOk()).andExpect(content().bytes(png()));
        var body=producto("Multimedia "+UUID.randomUUID(), foto);
        var result=mockMvc.perform(post("/api/v1/productos").header("Authorization","Bearer "+token).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated()).andReturn();
        long id=objectMapper.readTree(result.getResponse().getContentAsString()).path("id").asLong();
        mockMvc.perform(get(url)).andExpect(status().isOk()).andExpect(content().contentType("image/png"));
        mockMvc.perform(head(url)).andExpect(status().isOk()).andExpect(header().string("Content-Length", String.valueOf(png().length)));
        mockMvc.perform(get(url).header("Range","bytes=0-3")).andExpect(status().isPartialContent()).andExpect(content().bytes(Arrays.copyOf(png(),4)));
        mockMvc.perform(get(url).header("Range","bytes=999999-")).andExpect(status().isRequestedRangeNotSatisfiable());
        // Un cliente anterior puede guardar la URL sin conocer el nuevo ID.
        body.remove("imagenArchivoId");
        mockMvc.perform(put("/api/v1/productos/"+id).header("Authorization","Bearer "+token).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(body))).andExpect(status().isOk());
        mockMvc.perform(get(url)).andExpect(status().isOk());
        var nueva=subir(token); body.put("imagenArchivoId", nueva.path("id").asText()); body.put("imagenUrl", nueva.path("url").asText());
        mockMvc.perform(put("/api/v1/productos/"+id).header("Authorization","Bearer "+token).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(body))).andExpect(status().isOk());
        assertEquals("RETIRADO", archivos.findById(UUID.fromString(foto.path("id").asText())).orElseThrow().getEstado());
        mockMvc.perform(get(url)).andExpect(status().isNotFound());
        mockMvc.perform(get(nueva.path("url").asText())).andExpect(status().isOk());
        mockMvc.perform(delete("/api/v1/archivos/"+nueva.path("id").asText()).header("Authorization","Bearer "+token)).andExpect(status().isNotFound());
    }
    @Test void rechazaContenidoFalsoYPermisosInsuficientes() throws Exception {
        var falso=new MockMultipartFile("archivo","foto.jpg","image/jpeg","<html>no es foto</html>".getBytes());
        mockMvc.perform(multipart("/api/v1/archivos").file(falso)).andExpect(status().isUnauthorized());
        String cliente=tokenCliente("media-cliente@test.com","ClaveCliente123");
        mockMvc.perform(multipart("/api/v1/archivos").file(falso).header("Authorization","Bearer "+cliente)).andExpect(status().isForbidden());
        mockMvc.perform(multipart("/api/v1/archivos").file(falso).header("Authorization","Bearer "+tokenAdmin())).andExpect(status().isUnprocessableEntity());
    }
    @Test void almacenamientoCaidoNoCreaReferenciaYResponde503() throws Exception {
        long antes=archivos.count();
        doThrow(new AlmacenamientoNoDisponible(new IOException("sin conexion"))).when(almacen).guardar(any(),anyString());
        mockMvc.perform(multipart("/api/v1/archivos").file(new MockMultipartFile("archivo","foto.png","image/png",png())).header("Authorization","Bearer "+tokenAdmin())).andExpect(status().isServiceUnavailable());
        assertEquals(antes, archivos.count());
    }
    @Test void limpiaTemporalesYProtegeLosDeOtrosUsuarios() throws Exception {
        String token=tokenAdmin(); var foto=subir(token); UUID id=UUID.fromString(foto.path("id").asText());
        String cliente=tokenCliente("media-otro@test.com","ClaveCliente123");
        mockMvc.perform(get(foto.path("url").asText()).header("Authorization","Bearer "+cliente)).andExpect(status().isNotFound());
        jdbc.update("update archivos.archivos set modificado_en = now() - interval '2 days' where id = ?", id);
        limpieza.limpiar();
        assertFalse(archivos.existsById(id));
    }
}
