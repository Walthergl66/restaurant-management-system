package com.restaurante.pagos;

import com.restaurante.AbstractIntegracionApi;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Fase 5 (RF-26 a RF-35): cobro mixto de cuentas con caja, comprobante con
 * numeración secuencial y cierre de caja conciliado. No hay caja → no se cobra;
 * el total de pagos debe coincidir con el total de la cuenta.
 */
class CobroFacturacionCajaIntegrationTest extends AbstractIntegracionApi {

    private String administradorToken;
    private String meseroToken;
    private String cajeroToken;

    @BeforeEach
    void preparar() {
        administradorToken = tokenAdmin();
        meseroToken = tokenMesero();
        cajeroToken = asegurarUsuario("cajero_f5", "CAJERO", "clave789");
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
    void cobrarSinCajaAbiertaSeRechaza() throws Exception {
        long mesaId = crearMesa();
        long productoId = crearProducto("Pollo", "5.00", null);
        String codigo = crearBorrador(mesaId);
        postItem(codigo, productoId, 1);
        confirmar(codigo);
        long cuentaId = cuentaAbiertaDe(mesaId);

        mockMvc.perform(post("/api/v1/cuentas/" + cuentaId + "/cobros")
                        .header("Authorization", "Bearer " + cajeroToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"pagos":[{"metodo":"EFECTIVO","monto":5.00}],"documento":null}
                                """))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void cobroMixtoCierraCuentaYEmiteComprobanteSecuencial() throws Exception {
        abrirCaja("10.00");

        long mesaId = crearMesa();
        long productoId = crearProducto("Pollo Entero", "10.00", null);
        String codigo = crearBorrador(mesaId);
        postItem(codigo, productoId, 2);
        confirmar(codigo);
        long cuentaId = cuentaAbiertaDe(mesaId);

        MvcResult cobro = mockMvc.perform(post("/api/v1/cuentas/" + cuentaId + "/cobros")
                        .header("Authorization", "Bearer " + cajeroToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"pagos":[{"metodo":"EFECTIVO","monto":12.00},{"metodo":"TARJETA","monto":8.00}],
                                 "documento":{"tipo":"FACTURA","clienteNombre":"Juan Pérez","clienteIdentificacion":"1722222222"}}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.total").value(20.00))
                .andExpect(jsonPath("$.pagos.length()").value(2))
                .andExpect(jsonPath("$.comprobanteCorrelativo").value("R-000001"))
                .andReturn();

        JsonNode body = objectMapper.readTree(cobro.getResponse().getContentAsString());
        assertEquals(2, body.path("pagos").size());

        // Cuenta cerrada y mesa libre
        mockMvc.perform(get("/api/v1/cuentas/" + cuentaId)
                        .header("Authorization", "Bearer " + administradorToken))
                .andExpect(jsonPath("$.estado").value("CERRADA"));
        assertTrue(getMesa(mesaId).contains("\"estado\":\"LIBRE\""));

        // Comprobante emitido con datos del cliente
        String comprobantes = mockMvc.perform(get("/api/v1/comprobantes?cuentaId=" + cuentaId)
                        .header("Authorization", "Bearer " + cajeroToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertTrue(comprobantes.contains("R-000001"));
        assertTrue(comprobantes.contains("Juan Pérez"));
        assertTrue(comprobantes.contains("FACTURA"));

        // Dos movimientos de ingreso en la caja
        long cajaId = body.path("cajaId").asLong();
        String movimientos = mockMvc.perform(get("/api/v1/cajas/" + cajaId + "/movimientos")
                        .header("Authorization", "Bearer " + cajeroToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode movs = objectMapper.readTree(movimientos);
        assertEquals(2, movs.size());
        assertTrue(movimientos.contains("\"tipo\":\"INGRESO\""));
    }

    @Test
    void cobrarSumaDistintaAlTotalSeRechaza() throws Exception {
        abrirCaja("0.00");

        long mesaId = crearMesa();
        long productoId = crearProducto("Seco", "6.00", null);
        String codigo = crearBorrador(mesaId);
        postItem(codigo, productoId, 1);
        confirmar(codigo);
        long cuentaId = cuentaAbiertaDe(mesaId);

        mockMvc.perform(post("/api/v1/cuentas/" + cuentaId + "/cobros")
                        .header("Authorization", "Bearer " + cajeroToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"pagos":[{"metodo":"EFECTIVO","monto":5.00}]}
                                """))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void cobrarCuentaYaCerradaSeRechaza() throws Exception {
        abrirCaja("0.00");
        long mesaId = crearMesa();
        long productoId = crearProducto("Caldo", "3.00", null);
        String codigo = crearBorrador(mesaId);
        postItem(codigo, productoId, 1);
        confirmar(codigo);
        long cuentaId = cuentaAbiertaDe(mesaId);

        mockMvc.perform(post("/api/v1/cuentas/" + cuentaId + "/cobros")
                        .header("Authorization", "Bearer " + cajeroToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"pagos":[{"metodo":"EFECTIVO","monto":3.00}]}
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/cuentas/" + cuentaId + "/cobros")
                        .header("Authorization", "Bearer " + cajeroToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"pagos":[{"metodo":"EFECTIVO","monto":3.00}]}
                                """))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void aperturaDobleYSegundoCobroSinCaja() throws Exception {
        abrirCaja("50.00");
        mockMvc.perform(post("/api/v1/cajas/apertura")
                        .header("Authorization", "Bearer " + cajeroToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"montoInicial\":5.00}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void cierreConciliadoRegistraDiferencia() throws Exception {
        abrirCaja("10.00");

        long mesaId = crearMesa();
        long productoId = crearProducto("Sopa", "20.00", null);
        String codigo = crearBorrador(mesaId);
        postItem(codigo, productoId, 1);
        confirmar(codigo);
        long cuentaId = cuentaAbiertaDe(mesaId);

        MvcResult cobro = mockMvc.perform(post("/api/v1/cuentas/" + cuentaId + "/cobros")
                        .header("Authorization", "Bearer " + cajeroToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"pagos":[{"metodo":"EFECTIVO","monto":20.00}]}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        long cajaId = objectMapper.readTree(cobro.getResponse().getContentAsString())
                .path("cajaId").asLong();

        // apertura 10 + cobro 20 = 30 esperado; se declaran 30 reales → sin diferencia
        mockMvc.perform(post("/api/v1/cajas/" + cajaId + "/cierre")
                        .header("Authorization", "Bearer " + cajeroToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"montoReal\":30.00}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CERRADA"))
                .andExpect(jsonPath("$.diferencia").value(0.00));
    }

    // --- utilidades ---

    private void abrirCaja(String monto) throws Exception {
        mockMvc.perform(post("/api/v1/cajas/apertura")
                        .header("Authorization", "Bearer " + cajeroToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"montoInicial\":" + monto + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("ABIERTA"));
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
                        .header("Idempotency-Key", "clave-f5-" + codigo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CONFIRMADO"));
    }

    private String crearBorrador(long mesaId) throws Exception {
        String codigo = "F5-" + (System.nanoTime() % 1000000);
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
                                """.formatted((int) (2000 + System.nanoTime() % 5000))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).path("id").asLong();
    }

    private long crearProducto(String nombre, String precio, Long areaId) throws Exception {
        String area = areaId == null ? "null" : String.valueOf(areaId);
        String body = mockMvc.perform(post("/api/v1/productos")
                        .header("Authorization", "Bearer " + administradorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"%s-%d","precio":%s,"categoriaId":null,"areaId":%s,"extraIds":[],"ingredientes":[]}
                                """.formatted(nombre, System.nanoTime() % 100000, precio, area)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).path("id").asLong();
    }

    private String getMesa(long mesaId) throws Exception {
        return mockMvc.perform(get("/api/v1/mesas/" + mesaId)
                        .header("Authorization", "Bearer " + administradorToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
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