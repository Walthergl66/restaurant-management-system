package com.restaurante.fase6;

import com.restaurante.AbstractIntegracionApi;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Fase 6 (RF-36 a RF-39, RF-51, RF-52, RNF-07): finanzas que agregan
 * ingresos/egresos en el período, reportes de ventas que descuentan las
 * anulaciones aprobadas y auditoría solo-lectura que registra los eventos de
 * negocio. Mesero no ve finanzas, reportes ni auditoría.
 *
 * <p>El contenedor de BD se comparte entre todas las clases de test: cada
 * prueba aísla sus datos con una ventana de tiempo propia ([inicio - 5s,
 * 2030]) y nombres únicos, en vez de rangos abiertos.
 */
class AuditoriaFinanzasReportesApiIntegrationTest extends AbstractIntegracionApi {

    private String administradorToken;
    private String meseroToken;
    private String cajeroToken;

    @BeforeEach
    void prepararTokens() {
        administradorToken = tokenAdmin();
        meseroToken = tokenMesero();
        cajeroToken = asegurarUsuario("cajero_f6", "CAJERO", "clave678");
    }

    @AfterEach
    void limpiarCaja() {
        // La caja es un recurso único (una abierta a la vez y el contenedor se
        // comparte entre tests): cada test deja su turno cerrado para no
        // interferir con el siguiente.
        try {
            MvcResult abierta = mockMvc.perform(get("/api/v1/cajas/abierta")
                            .header("Authorization", "Bearer " + cajeroToken))
                    .andExpect(result -> {
                        int status = result.getResponse().getStatus();
                        if (status != 200 && status != 404) {
                            throw new AssertionError("Esperaba 200 o 404 al consultar caja abierta, fue " + status);
                        }
                    })
                    .andReturn();
            if (abierta.getResponse().getStatus() == 200) {
                long cajaId = objectMapper.readTree(abierta.getResponse().getContentAsString())
                        .path("id").asLong();
                mockMvc.perform(post("/api/v1/cajas/" + cajaId + "/cierre")
                                .header("Authorization", "Bearer " + cajeroToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"montoReal\":0}"))
                        .andExpect(status().isOk());
            }
        } catch (Exception ignorada) {
            // no enmascarar el resultado de la prueba
        }
    }

    @Test
    void cobrarAlimentaFinanzasReporteYAuditoria() throws Exception {
        Instant inicio = Instant.now().minusSeconds(5);
        String desde = inicio.toString();
        String hasta = "2030-01-01T00:00:00Z";
        // La BD se comparte con las demás pruebas: las finanzas agregan todo
        // lo del período, así que se mide el DELTA que aporta este cobro.
        JsonNode base = finanzas("2020-01-01T00:00:00Z", hasta);

        abrirCaja("10.00");
        long mesaId = crearMesa();
        long productoId = crearProducto("F6Plato", "18.00");
        String codigo = crearBorrador(mesaId);
        postItem(codigo, productoId, 1);
        confirmar(codigo);
        long cuentaId = cuentaAbiertaDe(mesaId);
        cobrar(cuentaId, "18.00");

        JsonNode resumen = finanzas("2020-01-01T00:00:00Z", hasta);
        BigDecimal deltaIngresos = resumen.path("ingresos").decimalValue()
                .subtract(base.path("ingresos").decimalValue());
        BigDecimal deltaEgresos = resumen.path("egresos").decimalValue()
                .subtract(base.path("egresos").decimalValue());
        BigDecimal deltaResultado = resumen.path("resultado").decimalValue()
                .subtract(base.path("resultado").decimalValue());
        assertEquals(0, new BigDecimal("18.00").compareTo(deltaIngresos), "El cobro suma a ingresos");
        assertEquals(0, new BigDecimal("0.00").compareTo(deltaEgresos), "Sin egresos en el flujo");
        assertEquals(0, new BigDecimal("18.00").compareTo(deltaResultado), "Resultado del cobro");

        String ventas = mockMvc.perform(get("/api/v1/reportes/ventas")
                        .header("Authorization", "Bearer " + administradorToken)
                        .param("desde", desde)
                        .param("hasta", hasta))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode porProducto = objectMapper.readTree(ventas).path("porProducto");
        boolean encontrado = false;
        for (JsonNode linea : porProducto) {
            if (linea.path("nombre").asText().startsWith("F6Plato")) {
                assertEquals(1, linea.path("cantidad").asInt());
                assertEquals(0, new BigDecimal("18.00").compareTo(linea.path("monto").decimalValue()));
                encontrado = true;
            }
        }
        assertTrue(encontrado, "F6Plato debe aparecer en el reporte");

        String auditoria = mockMvc.perform(get("/api/v1/auditoria")
                        .header("Authorization", "Bearer " + administradorToken)
                        .param("desde", desde)
                        .param("hasta", hasta)
                        .param("entidad", "CUENTA"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        boolean cobroAuditado = false;
        for (JsonNode evento : objectMapper.readTree(auditoria)) {
            if ("CUENTA_COBRADA".equals(evento.path("tipo").asText())
                    && String.valueOf(cuentaId).equals(evento.path("entidadId").asText())) {
                cobroAuditado = true;
            }
        }
        assertTrue(cobroAuditado, "El cobro debe quedar en la auditoría");
    }

    @Test
    void reporteVentasDescuentaAnulacionAprobada() throws Exception {
        Instant inicio = Instant.now().minusSeconds(5);
        String desde = inicio.toString();
        String hasta = "2030-01-01T00:00:00Z";

        abrirCaja("10.00");
        long mesaId = crearMesa();
        long platoId = crearProducto("F6Anulable", "8.00");
        String codigo = crearBorrador(mesaId);
        postItem(codigo, platoId, 2);
        confirmar(codigo);
        long lineaId = lineaIdDe(codigo);
        long cuentaId = cuentaAbiertaDe(mesaId);

        // Se solicita anular una de las dos unidades (8.00) y se aprueba: la
        // cuenta queda en 8.00 y el reporte descuenta la aprobada.
        mockMvc.perform(post("/api/v1/pedidos/" + codigo + "/items/" + lineaId + "/anulaciones")
                        .header("Authorization", "Bearer " + meseroToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cantidad\":1,\"motivo\":\"se cayó el plato\"}"))
                .andExpect(status().isCreated());
        long anulacionId = anulacionSolicitadaDe(codigo, lineaId);
        mockMvc.perform(patch("/api/v1/anulaciones/" + anulacionId + "/aprobar")
                        .header("Authorization", "Bearer " + administradorToken))
                .andExpect(status().isOk());

        cobrar(cuentaId, "8.00");

        String ventas = mockMvc.perform(get("/api/v1/reportes/ventas")
                        .header("Authorization", "Bearer " + administradorToken)
                        .param("desde", desde)
                        .param("hasta", hasta))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        boolean encontrado = false;
        for (JsonNode linea : objectMapper.readTree(ventas).path("porProducto")) {
            if (linea.path("nombre").asText().startsWith("F6Anulable")) {
                assertEquals(1, linea.path("cantidad").asInt(), "La unidad anulada no cuenta");
                assertEquals(0, new BigDecimal("8.00").compareTo(linea.path("monto").decimalValue()),
                        "Anulación aprobada descuenta del reporte");
                encontrado = true;
            }
        }
        assertTrue(encontrado, "F6Anulable debe aparecer en el reporte");
    }

    @Test
    void meseroNoVeAuditoriaFinanzasNiReportes() throws Exception {
        String desde = "2020-01-01T00:00:00Z";
        String hasta = "2030-01-01T00:00:00Z";
        mockMvc.perform(get("/api/v1/auditoria")
                        .header("Authorization", "Bearer " + meseroToken)
                        .param("desde", desde)
                        .param("hasta", hasta))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/finanzas/resumen")
                        .header("Authorization", "Bearer " + meseroToken)
                        .param("desde", desde)
                        .param("hasta", hasta))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/reportes/ventas")
                        .header("Authorization", "Bearer " + meseroToken)
                        .param("desde", desde)
                        .param("hasta", hasta))
                .andExpect(status().isForbidden());
    }

    // --- utilidades (mismo flujo probado en Fase 5, con prefijo F6) ---

    private JsonNode finanzas(String desde, String hasta) throws Exception {
        String body = mockMvc.perform(get("/api/v1/finanzas/resumen")
                        .header("Authorization", "Bearer " + administradorToken)
                        .param("desde", desde)
                        .param("hasta", hasta))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body);
    }

    private void abrirCaja(String monto) throws Exception {
        mockMvc.perform(post("/api/v1/cajas/apertura")
                        .header("Authorization", "Bearer " + cajeroToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"montoInicial\":" + monto + "}"))
                .andExpect(status().isCreated());
    }

    private void cobrar(long cuentaId, String monto) throws Exception {
        mockMvc.perform(post("/api/v1/cuentas/" + cuentaId + "/cobros")
                        .header("Authorization", "Bearer " + cajeroToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pagos\":[{\"metodo\":\"EFECTIVO\",\"monto\":" + monto + "}]}"))
                .andExpect(status().isCreated());
    }

    private long cuentaAbiertaDe(long mesaId) throws Exception {
        String body = mockMvc.perform(get("/api/v1/cuentas?mesaId=" + mesaId)
                        .header("Authorization", "Bearer " + administradorToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        for (JsonNode cuenta : objectMapper.readTree(body)) {
            if ("ABIERTA".equals(cuenta.path("estado").asText())) {
                return cuenta.path("id").asLong();
            }
        }
        throw new AssertionError("No hay cuenta abierta para la mesa");
    }

    private long lineaIdDe(String codigo) throws Exception {
        String body = mockMvc.perform(get("/api/v1/pedidos/" + codigo)
                        .header("Authorization", "Bearer " + administradorToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode lineas = objectMapper.readTree(body).path("lineas");
        if (lineas.isEmpty()) {
            throw new AssertionError("El pedido no tiene líneas: " + codigo);
        }
        return lineas.get(0).path("id").asLong();
    }

    private long anulacionSolicitadaDe(String codigo, long lineaId) throws Exception {
        String body = mockMvc.perform(get("/api/v1/anulaciones?pedidoCodigo=" + codigo)
                        .header("Authorization", "Bearer " + administradorToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        for (JsonNode anulacion : objectMapper.readTree(body)) {
            if (anulacion.path("lineaId").asLong() == lineaId
                    && "SOLICITADA".equals(anulacion.path("estado").asText())) {
                return anulacion.path("id").asLong();
            }
        }
        throw new AssertionError("No hay anulación solicitada para la línea " + lineaId);
    }

    private void postItem(String codigo, long productoId, int cantidad) throws Exception {
        mockMvc.perform(post("/api/v1/pedidos/" + codigo + "/items")
                        .header("Authorization", "Bearer " + meseroToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productoId":%d,"cantidad":%d,"extraIds":[],"ingredientesRemovidos":[]}
                                """.formatted(productoId, cantidad)))
                .andExpect(status().isCreated());
    }

    private void confirmar(String codigo) throws Exception {
        mockMvc.perform(post("/api/v1/pedidos/" + codigo + "/confirmar")
                        .header("Authorization", "Bearer " + meseroToken)
                        .header("Idempotency-Key", "clave-f6-" + codigo))
                .andExpect(status().isOk());
    }

    private String crearBorrador(long mesaId) throws Exception {
        String codigo = "F6-" + (System.nanoTime() % 1000000);
        mockMvc.perform(post("/api/v1/pedidos")
                        .header("Authorization", "Bearer " + meseroToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"codigo":"%s","mesaId":%d}
                                """.formatted(codigo, mesaId)))
                .andExpect(status().isCreated());
        return codigo;
    }

    private long crearMesa() throws Exception {
        String body = mockMvc.perform(post("/api/v1/mesas")
                        .header("Authorization", "Bearer " + administradorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"numero":%d,"capacidad":4}
                                """.formatted((int) (6000 + System.nanoTime() % 5000))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).path("id").asLong();
    }

    private long crearProducto(String nombre, String precio) throws Exception {
        String body = mockMvc.perform(post("/api/v1/productos")
                        .header("Authorization", "Bearer " + administradorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"%s-%d","precio":%s,"categoriaId":null,"areaId":null,"extraIds":[],"ingredientes":[]}
                                """.formatted(nombre, System.nanoTime() % 100000, precio)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).path("id").asLong();
    }

    private String asegurarUsuario(String username, String rol, String password) {
        try {
            mockMvc.perform(post("/api/v1/usuarios")
                            .header("Authorization", "Bearer " + administradorToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"username":"%s","nombre":"%s","password":"%s","rolCodigo":"%s"}
                                    """.formatted(username, username, password, rol)))
                    .andExpect(result -> {
                        int status = result.getResponse().getStatus();
                        if (status != 201 && status != 409) {
                            throw new AssertionError("Esperaba 201 o 409, fue " + status);
                        }
                    });
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo asegurar el usuario " + username, e);
        }
        return loginToken(username, password);
    }
}
