package com.restaurante.archivos;

import com.restaurante.catalogo.web.dto.ProductoRequest;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import static org.junit.jupiter.api.Assertions.*;

class ProductoRequestCompatibilidadTest {
    @Test void aceptaClienteSinCamposMultimediaNuevos() throws Exception {
        ProductoRequest request=new ObjectMapper().readValue("""
                {"nombre":"Producto anterior","precio":1.50,"extraIds":[],"ingredientes":[]}
                """, ProductoRequest.class);
        assertEquals("Producto anterior", request.nombre());
        assertNull(request.quitarVideo()); assertNull(request.imagenArchivoId());
    }
}
