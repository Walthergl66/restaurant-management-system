package com.restaurante.cuentas;

import com.restaurante.AbstractIntegracionApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Fase 4: adiciones ligadas a la misma cuenta, anulaciones que descuentan solo
 * al aprobarse y el total SIEMPRE calculado desde los registros (RNF-16).
 */
class AnulacionesCuentasIntegrationTest extends AbstractIntegracionApi {

    private String administradorToken;
    private String meseroToken;
    private String cajeroToken;

    @BeforeEach
    void preparar() {
        administradorToken = tokenAdmin();
        meseroToken = tokenMesero();
        cajeroToken = asegurarUsuario("cajero_test", "CAJERO", "clave789");
    }

    @Test
    void adicionSeUneALaMismaCuentaYElTotalSuma() throws Exception {
        long mesaId = crearMesa();
        long sopa = crearProducto("Sopa", "6.00", null);
        long jugo = crearProducto("Jugo", "2.00", null);

        String primero = crearBorrador(mesaId);
        postItem(primero, sopa, 2);
        confirmar(primero);
        long cuentaId = cuentaAbiertaDe(mesaId);

        obtenerCuenta(cuentaId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(12.00))
                .andExpect(jsonPath("$.pedidos").isArray())
                .andExpect(jsonPath("$.pedidos.length()").value(1));

        String adicionCodigo = crearAdicion(cuentaId);
        postItem(adicionCodigo, jugo, 2);
        confirmar(adicionCodigo);

        MvcResult detalle = obtenerCuenta(cuentaId).andExpect(status().isOk()).andReturn();
        JsonNode cuenta = objectMapper.readTree(detalle.getResponse().getContentAsString());
        assertEquals(0, new BigDecimal("16.00").compareTo(cuenta.path("total").decimalValue()));
        assertEquals(2, cuenta.path("pedidos").size());

        String mesa = getMesa(mesaId);
        assertTrue(mesa.contains("\"estado\":\"OCUPADA\""), "La adición mantiene la mesa ocupada: " + mesa);
    }

    @Test
    void anulacionAprobadaDescuentaYGeneraComandaDeCancelacion() throws Exception {
        long areaId = crearArea();
        long mesaId = crearMesa();
        long productoId = crearProducto("Llapingacho", "4.00", areaId);

        String codigo = crearBorrador(mesaId);
        MvcResult item = postItemConRespuesta(codigo, productoId, 2);
        long lineaId = itemId(item);
        confirmar(codigo);
        long cuentaId = cuentaAbiertaDe(mesaId);

        solicitarAnulacion(codigo, lineaId, 1, "cliente lo rechazó")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("SOLICITADA"));

        aprobarAnulacionDesdeMesa(codigo, lineaId);

        obtenerCuenta(cuentaId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(4.00))
                .andExpect(jsonPath("$.anulaciones.length()").value(1));

        String comandas = mockMvc.perform(get("/api/v1/comandas")
                        .header("Authorization", "Bearer " + administradorToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertTrue(comandas.contains("\"pedidoCodigo\":\"" + codigo + "\""), "Comandas: " + comandas);
        assertTrue(comandas.contains("\"tipo\":\"CANCELACION\""), "Debe existir comanda de cancelación: " + comandas);

        String pendientes = mockMvc.perform(get("/api/v1/comandas/impresion/pendientes")
                        .header("Authorization", "Bearer " + administradorToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertTrue(pendientes.contains("comanda.cancelacion.impresion"),
                "La comanda de cancelación debe dejar una orden de impresión: " + pendientes);
    }

    @Test
    void anulacionRechazadaNoDescuenta() throws Exception {
        long mesaId = crearMesa();
        long productoId = crearProducto("Papa Pelada", "3.00", null);

        String codigo = crearBorrador(mesaId);
        MvcResult item = postItemConRespuesta(codigo, productoId, 2);
        long lineaId = itemId(item);
        confirmar(codigo);
        long cuentaId = cuentaAbiertaDe(mesaId);

        MvcResult solicitud = solicitarAnulacion(codigo, lineaId, 1, "cambio de opinión")
                .andExpect(status().isCreated())
                .andReturn();
        long anulacionId = objectMapper.readTree(solicitud.getResponse().getContentAsString()).path("id").asLong();

        mockMvc.perform(patch("/api/v1/anulaciones/" + anulacionId + "/rechazar")
                        .header("Authorization", "Bearer " + cajeroToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("RECHAZADA"));

        obtenerCuenta(cuentaId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(6.00))
                .andExpect(jsonPath("$.anulaciones").isEmpty());
    }

    @Test
    void noSePuedeAnularMasDeLaCantidadPedida() throws Exception {
        long mesaId = crearMesa();
        long productoId = crearProducto("Cangrejo", "7.00", null);

        String codigo = crearBorrador(mesaId);
        MvcResult item = postItemConRespuesta(codigo, productoId, 2);
        long lineaId = itemId(item);
        confirmar(codigo);

        solicitarAnulacion(codigo, lineaId, 3, "exceso")
                .andExpect(status().isUnprocessableEntity());
    }

    /**
     * Regresión del invariante RNF-16: la unidad ya comprometida en una
     * solicitud SOLICITADA no puede comprometerse otra vez. Antes solo se
     *zeecontaba lo aprobado, así que dos solicitudes de 1 sobre una línea de
     * 2 se aceptaban y, al aprobar ambas, el total de la cuenta quedaba
     * negativo.
     */
    @Test
    void noSePuedeComprometerDosVecesLaMismaUnidad() throws Exception {
        long mesaId = crearMesa();
        long productoId = crearProducto("Chuzo de Camarones", "8.00", null);

        String codigo = crearBorrador(mesaId);
        MvcResult item = postItemConRespuesta(codigo, productoId, 2);
        long lineaId = itemId(item);
        confirmar(codigo);
        long cuentaId = cuentaAbiertaDe(mesaId);

        // 1 de 2 queda comprometido por esta solicitud.
        MvcResult primera = solicitarAnulacion(codigo, lineaId, 1, "se deslizó al servir")
                .andExpect(status().isCreated())
                .andReturn();
        long primeraId = objectMapper.readTree(primera.getResponse().getContentAsString()).path("id").asLong();

        // Intentar comprometer la misma unidad otra vez debe rebotar: solo
        // queda 1 libre de 2.
        solicitarAnulacion(codigo, lineaId, 2, "misma unidad dos veces")
                .andExpect(status().isUnprocessableEntity());

        // La primera sigue siendo aprobable y descuenta lo suyo.
        mockMvc.perform(patch("/api/v1/anulaciones/" + primeraId + "/aprobar")
                        .header("Authorization", "Bearer " + cajeroToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("APROBADA"));

        // Total: 16.00 pedido - 8.00 anulado = 8.00 (no negativo).
        obtenerCuenta(cuentaId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(8.00));

        // Queda exactamente 1 unidad: se puede anular, y con eso se agota.
        MvcResult segunda = solicitarAnulacion(codigo, lineaId, 1, "última unidad")
                .andExpect(status().isCreated())
                .andReturn();
        long segundaId = objectMapper.readTree(segunda.getResponse().getContentAsString()).path("id").asLong();

        // Ya no queda nada anulable: la línea tenía 2 y están comprometidas las 2.
        solicitarAnulacion(codigo, lineaId, 1, "excede lo anulable")
                .andExpect(status().isUnprocessableEntity());

        // Al aprobar la segunda el total llega a 0.00 exactos: el máximo
        // anulable es lo pedido, y la cuenta nunca queda negativa.
        mockMvc.perform(patch("/api/v1/anulaciones/" + segundaId + "/aprobar")
                        .header("Authorization", "Bearer " + cajeroToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("APROBADA"));

        obtenerCuenta(cuentaId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0.00));
    }

    /**
     * Regresión del RNF-16 bajo concurrencia: dos cajeros aprueban al mismo
     * tiempo dos solicitudes sobre la misma línea. Con el saldo leído sin
     * bloqueo, ambos leen lo mismo y los dos aprueban, con lo que la cuenta
     * queda descuenciada por encima de lo pedido.
     *
     * El escenario es 1 unidad pedida y 2 solicitudes de 1: solo una puede
     * acabar APROBADA; la otra debe rebotar con 422.
     */
    @Test
    void solicitudesConcurrentesSobreLaUltimaUnidadSoloUnaProspera() throws Exception {
        long mesaId = crearMesa();
        long productoId = crearProducto("Ajiaco", "7.00", null);

        String codigo = crearBorrador(mesaId);
        MvcResult item = postItemConRespuesta(codigo, productoId, 1);
        long lineaId = itemId(item);
        confirmar(codigo);

        // Una sola unidad y dos meseros pidiendo su anulación a la vez. Ambas
        // solicitudes caben por separado contra el saldo, y en conjunto no.
        // Sin serializar la reserva, las dos leerían "comprometido 0" y las dos
        // pasarían, deixando la unidad prometida dos veces; al aprobarlas, la
        // cuenta quedaría descuenciada por debajo de lo consumido.
        int[] estados = enParalelo(
                () -> solicitarAnulacion(codigo, lineaId, 1, "primera"),
                () -> solicitarAnulacion(codigo, lineaId, 1, "segunda"));

        assertEquals(1, estados[0] == 201 ? 1 : 0,
                "Solo una solicitud debe crearse, la otra rebotar con 422. Estados: "
                        + estados[0] + " y " + estados[1]);
        assertEquals(201, estados[1] == 201 ? 201 : 422, estados[1]);
        assertTrue(estados[0] == 201 || estados[1] == 201,
                "Al menos una solicitud debe prosperar: " + estados[0] + " y " + estados[1]);
    }

    @Test
    void aprobacionesConcurrentesDeSolicitudesValidasNoInviertenElSaldo() throws Exception {
        long mesaId = crearMesa();
        long productoId = crearProducto("Sancocho", "6.00", null);

        String codigo = crearBorrador(mesaId);
        MvcResult item = postItemConRespuesta(codigo, productoId, 2);
        long lineaId = itemId(item);
        confirmar(codigo);
        long cuentaId = cuentaAbiertaDe(mesaId);

        // 2 unidades, 2 solicitudes de 1: cada una reserva la suya y juntas
        // agotan la línea exactamente. Las dos aprobaciones en paralelo son
        // legítimas y el total debe cerrar en cero.
        long idA = idDeAnulacion(solicitarAnulacion(codigo, lineaId, 1, "primera")
                .andExpect(status().isCreated()).andReturn());
        long idB = idDeAnulacion(solicitarAnulacion(codigo, lineaId, 1, "segunda")
                .andExpect(status().isCreated()).andReturn());

        int[] estados = enParalelo(
                () -> mockMvc.perform(patch("/api/v1/anulaciones/" + idA + "/aprobar")
                        .header("Authorization", "Bearer " + cajeroToken)),
                () -> mockMvc.perform(patch("/api/v1/anulaciones/" + idB + "/aprobar")
                        .header("Authorization", "Bearer " + cajeroToken)));

        assertEquals(200, estados[0], "Aprobación A: " + estados[0]);
        assertEquals(200, estados[1], "Aprobación B: " + estados[1]);

        // 2 unidades a 6.00 = 12.00, anuladas 2 unidades = 12.00.
        obtenerCuenta(cuentaId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0.00));
    }

    /**
     * Lanza las dos peticiones a la vez y devuelve su código de estado. Cada
     * petición corre en su propia transacción: las pruebas no son
     * transaccionales, así que el hilo que llama a la API abre y cierra
     * transacción por su cuenta.
     */
    private int[] enParalelo(PeticionConcurrente a, PeticionConcurrente b) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch arranque = new CountDownLatch(1);
        try {
            Future<Integer> fa = pool.submit(() -> {
                arranque.await(10, TimeUnit.SECONDS);
                return a.ejecutar().andReturn().getResponse().getStatus();
            });
            Future<Integer> fb = pool.submit(() -> {
                arranque.await(10, TimeUnit.SECONDS);
                return b.ejecutar().andReturn().getResponse().getStatus();
            });
            arranque.countDown();
            return new int[]{fa.get(30, TimeUnit.SECONDS), fb.get(30, TimeUnit.SECONDS)};
        } finally {
            pool.shutdownNow();
        }
    }

    @FunctionalInterface
    private interface PeticionConcurrente {
        ResultActions ejecutar() throws Exception;
    }

    private long idDeAnulacion(MvcResult solicitud) throws Exception {
        return objectMapper.readTree(solicitud.getResponse().getContentAsString()).path("id").asLong();
    }

    private int aprobarEnParalelo(long anulacionId, CountDownLatch arranque) throws Exception {
        arranque.await(10, TimeUnit.SECONDS);
        return mockMvc.perform(patch("/api/v1/anulaciones/" + anulacionId + "/aprobar")
                        .header("Authorization", "Bearer " + cajeroToken))
                .andReturn().getResponse().getStatus();
    }

    @Test
    void meseroNoApruebaAnulaciones() throws Exception {
        long mesaId = crearMesa();
        long productoId = crearProducto("Corvina", "9.00", null);

        String codigo = crearBorrador(mesaId);
        MvcResult item = postItemConRespuesta(codigo, productoId, 1);
        long lineaId = itemId(item);
        confirmar(codigo);

        MvcResult solicitud = solicitarAnulacion(codigo, lineaId, 1, "test")
                .andExpect(status().isCreated())
                .andReturn();
        long anulacionId = objectMapper.readTree(solicitud.getResponse().getContentAsString()).path("id").asLong();

        mockMvc.perform(patch("/api/v1/anulaciones/" + anulacionId + "/aprobar")
                        .header("Authorization", "Bearer " + meseroToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void cerrarCuentaExigeConfirmarTodoYLiberaLaMesa() throws Exception {
        long mesaId = crearMesa();
        long productoId = crearProducto("Bolón", "2.50", null);

        String primero = crearBorrador(mesaId);
        postItem(primero, productoId, 1);
        confirmar(primero);
        long cuentaId = cuentaAbiertaDe(mesaId);

        String adicion = crearAdicion(cuentaId);
        postItem(adicion, productoId, 1);
        mockMvc.perform(patch("/api/v1/cuentas/" + cuentaId + "/cerrar")
                        .header("Authorization", "Bearer " + cajeroToken))
                .andExpect(status().isUnprocessableEntity());

        confirmar(adicion);
        mockMvc.perform(patch("/api/v1/cuentas/" + cuentaId + "/cerrar")
                        .header("Authorization", "Bearer " + cajeroToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CERRADA"))
                .andExpect(jsonPath("$.total").value(5.00));

        String mesa = getMesa(mesaId);
        assertTrue(mesa.contains("\"estado\":\"LIBRE\""), "Cerrar la cuenta libera la mesa: " + mesa);
    }

    @Test
    void cancelarElUnicoBorradorCierraLaCuentaYLiberaLaMesa() throws Exception {
        long mesaId = crearMesa();
        long productoId = crearProducto("Tigrillo", "3.50", null);

        String codigo = crearBorrador(mesaId);
        postItem(codigo, productoId, 1);

        mockMvc.perform(delete("/api/v1/pedidos/" + codigo)
                        .header("Authorization", "Bearer " + meseroToken))
                .andExpect(status().isNoContent());

        String cuentas = mockMvc.perform(get("/api/v1/cuentas?mesaId=" + mesaId)
                        .header("Authorization", "Bearer " + administradorToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertTrue(cuentas.contains("\"estado\":\"CERRADA\""), "La cuenta vacía debe cerrarse: " + cuentas);
        assertTrue(getMesa(mesaId).contains("\"estado\":\"LIBRE\""));
    }

    @Test
    void cancelarAdicionNoLiberaLaMesaNiCierraLaCuenta() throws Exception {
        long mesaId = crearMesa();
        long productoId = crearProducto("Seco", "5.00", null);

        String primero = crearBorrador(mesaId);
        postItem(primero, productoId, 1);
        confirmar(primero);
        long cuentaId = cuentaAbiertaDe(mesaId);

        String adicion = crearAdicion(cuentaId);
        postItem(adicion, productoId, 2);
        mockMvc.perform(delete("/api/v1/pedidos/" + adicion)
                        .header("Authorization", "Bearer " + meseroToken))
                .andExpect(status().isNoContent());

        assertTrue(getMesa(mesaId).contains("\"estado\":\"OCUPADA\""),
                "La mesa sigue ocupada mientras haya pedido confirmado");
        String cuentas = mockMvc.perform(get("/api/v1/cuentas?mesaId=" + mesaId)
                        .header("Authorization", "Bearer " + administradorToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertTrue(cuentas.contains("\"estado\":\"ABIERTA\""), "Cuenta inválida tras cancelar adición: " + cuentas);
    }

    // --- utilidades ---

    private void postItem(String codigo, long productoId, int cantidad) throws Exception {
        mockMvc.perform(post("/api/v1/pedidos/" + codigo + "/items")
                        .header("Authorization", "Bearer " + meseroToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productoId":%d,"cantidad":%d,"extraIds":[],"ingredientesRemovidos":[]}
                                """.formatted(productoId, cantidad)))
                .andExpect(status().isCreated())
                .andReturn();
    }

    private MvcResult postItemConRespuesta(String codigo, long productoId, int cantidad) throws Exception {
        return mockMvc.perform(post("/api/v1/pedidos/" + codigo + "/items")
                        .header("Authorization", "Bearer " + meseroToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productoId":%d,"cantidad":%d,"extraIds":[],"ingredientesRemovidos":[]}
                                """.formatted(productoId, cantidad)))
                .andExpect(status().isCreated())
                .andReturn();
    }

    private long itemId(MvcResult item) throws Exception {
        return objectMapper.readTree(item.getResponse().getContentAsString())
                .path("lineas").get(0).path("id").asLong();
    }

    private void confirmar(String codigo) throws Exception {
        mockMvc.perform(post("/api/v1/pedidos/" + codigo + "/confirmar")
                        .header("Authorization", "Bearer " + meseroToken)
                        .header("Idempotency-Key", "clave-" + codigo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CONFIRMADO"));
    }

    private long cuentaDeMesaAbierta(long mesaId) throws Exception {
        String body = mockMvc.perform(get("/api/v1/cuentas?mesaId=" + mesaId)
                        .header("Authorization", "Bearer " + administradorToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode array = objectMapper.readTree(body);
        for (JsonNode cuenta : array) {
            if ("ABIERTA".equals(cuenta.path("estado").asText())) {
                return cuenta.path("id").asLong();
            }
        }
        throw new AssertionError("No hay cuenta abierta para la mesa: " + mesaId);
    }

    private long cuentaAbiertaDe(long mesaId) throws Exception {
        return cuentaDeMesaAbierta(mesaId);
    }

    private String crearAdicion(long cuentaId) throws Exception {
        String codigo = "ADC-" + (System.nanoTime() % 1000000);
        String body = mockMvc.perform(post("/api/v1/cuentas/" + cuentaId + "/adiciones")
                        .header("Authorization", "Bearer " + meseroToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"codigo":"%s","notas":"adición de prueba"}
                                """.formatted(codigo)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).path("pedidoCodigo").asText();
    }

    private org.springframework.test.web.servlet.ResultActions obtenerCuenta(long cuentaId) throws Exception {
        return mockMvc.perform(get("/api/v1/cuentas/" + cuentaId)
                .header("Authorization", "Bearer " + administradorToken));
    }

    private org.springframework.test.web.servlet.ResultActions solicitarAnulacion(String codigo, long lineaId,
                                                                                  int cantidad, String motivo) throws Exception {
        return mockMvc.perform(post("/api/v1/pedidos/" + codigo + "/items/" + lineaId + "/anulaciones")
                        .header("Authorization", "Bearer " + meseroToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"cantidad":%d,"motivo":"%s"}
                                """.formatted(cantidad, motivo)));
    }

    private void aprobarAnulacionDesdeMesa(String codigo, long lineaId) throws Exception {
        String lista = mockMvc.perform(get("/api/v1/anulaciones?pedidoCodigo=" + codigo)
                        .header("Authorization", "Bearer " + cajeroToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        long anulacionId = -1;
        for (JsonNode anulacion : objectMapper.readTree(lista)) {
            if (anulacion.path("lineaId").asLong() == lineaId
                    && "SOLICITADA".equals(anulacion.path("estado").asText())) {
                anulacionId = anulacion.path("id").asLong();
            }
        }
        if (anulacionId < 0) {
            throw new AssertionError("No hay anulación solicitada para la línea: " + lineaId);
        }
        mockMvc.perform(patch("/api/v1/anulaciones/" + anulacionId + "/aprobar")
                        .header("Authorization", "Bearer " + cajeroToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("APROBADA"));
    }

    private String getMesa(long mesaId) throws Exception {
        return mockMvc.perform(get("/api/v1/mesas/" + mesaId)
                        .header("Authorization", "Bearer " + administradorToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    private String crearBorrador(long mesaId) throws Exception {
        String codigo = "P-" + (System.nanoTime() % 1000000);
        mockMvc.perform(post("/api/v1/pedidos")
                        .header("Authorization", "Bearer " + meseroToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"codigo":"%s","mesaId":%d}
                                """.formatted(codigo, mesaId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("BORRADOR"));
        return codigo;
    }

    private long crearArea() throws Exception {
        String body = mockMvc.perform(post("/api/v1/areas")
                        .header("Authorization", "Bearer " + administradorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Cocina-%d","descripcion":"área"}
                                """.formatted(System.nanoTime() % 100000)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).path("id").asLong();
    }

    private long crearMesa() throws Exception {
        String body = mockMvc.perform(post("/api/v1/mesas")
                        .header("Authorization", "Bearer " + administradorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"numero":%d,"capacidad":4}
                                """.formatted((int) (1000 + System.nanoTime() % 5000))))
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
                                {"nombre":"%s-%d","precio":%s,"categoriaId":null,"areaId":%s,"extraIds":[],"ingredientes":["cebolla"]}
                                """.formatted(nombre, System.nanoTime() % 100000, precio, area)))
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