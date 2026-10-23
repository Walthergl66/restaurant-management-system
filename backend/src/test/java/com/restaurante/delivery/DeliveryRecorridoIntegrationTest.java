package com.restaurante.delivery;

import com.restaurante.AbstractIntegracionApi;
import com.restaurante.SoporteRegistro;
import com.restaurante.delivery.domain.Coordenada;
import com.restaurante.delivery.domain.ResultadoRuta;
import com.restaurante.delivery.infrastructure.OrsClient;
import com.restaurante.usuarios.EmisorCorreo;
import com.restaurante.usuarios.domain.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.JsonNode;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Fase 5 del módulo delivery: el REPARTIDOR opera sus rutas, recorridos y
 * paradas a través de NUESTRO backend (ORS está MOCKEADO: no se consume cuota
 * real). Se validan el ciclo individual y el de multientrega, la idempotencia
 * de la entrega, la doble confirmación, las incidencias, la PROPIEDAD de los
 * recursos, el guard de repartidor HABILITADO, la finalización solo con
 * paradas resueltas y el 503 cuando ORS no está disponible. También que el
 * estado de PREPARACIÓN del pedido del cliente no cambia por el flujo logístico.
 */
class DeliveryRecorridoIntegrationTest extends AbstractIntegracionApi {

    @MockitoBean
    private EmisorCorreo emisorCorreo;

    /** ORS simulado: geometría y orden fijos, sin red ni cuota real. */
    @MockitoBean
    private OrsClient orsClient;

    @Autowired
    private com.restaurante.clientes.infrastructure.PedidoClienteRepository pedidoClienteRepository;

    @Autowired
    private com.restaurante.delivery.application.ConfigRestauranteService configRestaurante;

    @BeforeEach
    void stubOrs() {
        Mockito.reset(orsClient);
        // Geocoding determinista: un punto por dirección.
        Mockito.when(orsClient.geocodeDireccion(anyString()))
                .thenReturn(Optional.of(Coordenada.of(-0.19, -78.48)));
        // El origen de las rutas ahora sale de la config persistida del
        // restaurante (ya no de env vars). Se fija un punto para los tests.
        configRestaurante.fijarCoordenadasManual(
                java.math.BigDecimal.valueOf(-0.180653),
                java.math.BigDecimal.valueOf(-78.467834), "test");
        // Directions: geometría y totales fijos.
        Mockito.when(orsClient.calcularRuta(any(), any()))
                .thenReturn(new ResultadoRuta("geometria-fake", 1500, 600));
        // Optimization: invierte el orden recibido (prueba que el orden se respeta).
        Mockito.when(orsClient.optimizarParadas(any(), any(), any()))
                .thenAnswer(inv -> {
                    List<?> destinos = inv.getArgument(1);
                    int n = destinos.size();
                    java.util.List<Integer> orden = new java.util.ArrayList<>();
                    for (int i = n - 1; i >= 0; i--) {
                        orden.add(i);
                    }
                    return orden;
                });
    }

    // ---------- CICLO INDIVIDUAL ----------

    @Test
    void cicloIndividualCrearIniciarEntregarFinalizar() throws Exception {
        String admin = tokenAdmin();
        Repartidor r = repartidorConToken();
        long pedidoId = pedidoDomicilioListo();
        long asignacionId = asignar(admin, pedidoId, r.id);
        String estadoPrepAntes = estadoPedido(pedidoId);

        // Ruta individual: usa ORS (mock) y devuelve geometría.
        mockMvc.perform(post("/api/v1/delivery/rutas/individual")
                        .header("Authorization", "Bearer " + r.token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"asignacionId":%d}
                                """.formatted(asignacionId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.geometria").value("geometria-fake"))
                .andExpect(jsonPath("$.distanciaM").value(1500));

        long recorridoId = crearRecorrido(r.token, "INDIVIDUAL", List.of(asignacionId));
        long paradaId = primeraParada(r.token, recorridoId);

        // Iniciar: EN_CURSO y asignación EN_RUTA.
        mockMvc.perform(post("/api/v1/delivery/recorridos/{id}/iniciar", recorridoId)
                        .header("Authorization", "Bearer " + r.token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("EN_CURSO"));

        // Entregar la parada.
        mockMvc.perform(post("/api/v1/delivery/paradas/{id}/entregada", paradaId)
                        .header("Authorization", "Bearer " + r.token)
                        .header("Idempotency-Key", "k-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ENTREGADA"));

        // Finalizar: todas resueltas -> FINALIZADO.
        mockMvc.perform(post("/api/v1/delivery/recorridos/{id}/finalizar", recorridoId)
                        .header("Authorization", "Bearer " + r.token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("FINALIZADO"));

        // Aparece en historial.
        mockMvc.perform(get("/api/v1/delivery/recorridos/historial")
                        .header("Authorization", "Bearer " + r.token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.recorridoId == " + recorridoId + ")].estado").value(
                        org.hamcrest.Matchers.hasItem("FINALIZADO")));

        // El estado de PREPARACIÓN del pedido NO cambió por el flujo logístico.
        org.junit.jupiter.api.Assertions.assertEquals(estadoPrepAntes, estadoPedido(pedidoId));
    }

    @Test
    void entregadaEsIdempotentePorMismaKey() throws Exception {
        String admin = tokenAdmin();
        Repartidor r = repartidorConToken();
        long asignacionId = asignar(admin, pedidoDomicilioListo(), r.id);
        long recorridoId = crearRecorrido(r.token, "INDIVIDUAL", List.of(asignacionId));
        long paradaId = primeraParada(r.token, recorridoId);
        iniciar(r.token, recorridoId);

        entregar(r.token, paradaId, "repeat").andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ENTREGADA"));
        // Reintento con la MISMA key: no re-aplica, devuelve el estado actual (200).
        entregar(r.token, paradaId, "repeat").andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ENTREGADA"));
    }

    @Test
    void dobleConfirmacionDeParadaFallidaDa422() throws Exception {
        String admin = tokenAdmin();
        Repartidor r = repartidorConToken();
        long asignacionId = asignar(admin, pedidoDomicilioListo(), r.id);
        long recorridoId = crearRecorrido(r.token, "INDIVIDUAL", List.of(asignacionId));
        long paradaId = primeraParada(r.token, recorridoId);
        iniciar(r.token, recorridoId);

        // Marca incidencia (FALLIDA) y luego intenta entregar: 422.
        incidencia(r.token, paradaId, "cliente ausente").andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("FALLIDA"));
        entregar(r.token, paradaId, "k")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.type").value("urn:problem:restaurante:regla-de-negocio"));
    }

    @Test
    void incidenciaMarcaFallidaYSeConserva() throws Exception {
        String admin = tokenAdmin();
        Repartidor r = repartidorConToken();
        long asignacionId = asignar(admin, pedidoDomicilioListo(), r.id);
        long recorridoId = crearRecorrido(r.token, "INDIVIDUAL", List.of(asignacionId));
        long paradaId = primeraParada(r.token, recorridoId);
        iniciar(r.token, recorridoId);

        incidencia(r.token, paradaId, "direccion incorrecta").andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("FALLIDA"));

        // Con la parada resuelta (FALLIDA) el recorrido puede finalizar.
        mockMvc.perform(post("/api/v1/delivery/recorridos/{id}/finalizar", recorridoId)
                        .header("Authorization", "Bearer " + r.token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("FINALIZADO"));
    }

    @Test
    void finalizarConParadaPendienteDa422() throws Exception {
        String admin = tokenAdmin();
        Repartidor r = repartidorConToken();
        long asignacionId = asignar(admin, pedidoDomicilioListo(), r.id);
        long recorridoId = crearRecorrido(r.token, "INDIVIDUAL", List.of(asignacionId));
        iniciar(r.token, recorridoId);

        mockMvc.perform(post("/api/v1/delivery/recorridos/{id}/finalizar", recorridoId)
                        .header("Authorization", "Bearer " + r.token))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.type").value("urn:problem:restaurante:regla-de-negocio"));
    }

    // ---------- MULTIENTREGA ----------

    @Test
    void multientregaOptimizaCreaEntregaYFinaliza() throws Exception {
        String admin = tokenAdmin();
        Repartidor r = repartidorConToken();
        long a1 = asignar(admin, pedidoDomicilioListo(), r.id);
        long a2 = asignar(admin, pedidoDomicilioListo(), r.id);

        // Optimizar: el mock invierte el orden -> propuesta [a2, a1].
        MvcResult opt = mockMvc.perform(post("/api/v1/delivery/rutas/optimizar")
                        .header("Authorization", "Bearer " + r.token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"asignacionIds":[%d,%d],"criterio":"DURACION"}
                                """.formatted(a1, a2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paradas.length()").value(2))
                .andExpect(jsonPath("$.paradas[0].orden").value(0))
                .andReturn();
        JsonNode propuesta = objectMapper.readTree(opt.getResponse().getContentAsString());
        long primero = propuesta.path("paradas").get(0).path("asignacionId").asLong();
        long segundo = propuesta.path("paradas").get(1).path("asignacionId").asLong();
        org.junit.jupiter.api.Assertions.assertEquals(a2, primero);
        org.junit.jupiter.api.Assertions.assertEquals(a1, segundo);

        long recorridoId = crearRecorrido(r.token, "MULTIPLE", List.of(primero, segundo));
        iniciar(r.token, recorridoId);

        // Entrega cada parada por separado.
        for (long paradaId : paradas(r.token, recorridoId)) {
            entregar(r.token, paradaId, "k-" + paradaId).andExpect(status().isOk());
        }

        mockMvc.perform(post("/api/v1/delivery/recorridos/{id}/finalizar", recorridoId)
                        .header("Authorization", "Bearer " + r.token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("FINALIZADO"));
    }

    @Test
    void optimizarIncluyeCoordenadasDeParadaYOrigen() throws Exception {
        String admin = tokenAdmin();
        Repartidor r = repartidorConToken();
        long a1 = asignar(admin, pedidoDomicilioListo(), r.id);
        long a2 = asignar(admin, pedidoDomicilioListo(), r.id);

        mockMvc.perform(post("/api/v1/delivery/rutas/optimizar")
                        .header("Authorization", "Bearer " + r.token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"asignacionIds":[%d,%d],"criterio":"DURACION"}
                                """.formatted(a1, a2)))
                .andExpect(status().isOk())
                // El geocoding simulado devuelve (-0.19, -78.48) para cada destino.
                .andExpect(jsonPath("$.paradas[0].lat").value(-0.19))
                .andExpect(jsonPath("$.paradas[0].lng").value(-78.48))
                .andExpect(jsonPath("$.paradas[1].lat").value(-0.19))
                .andExpect(jsonPath("$.paradas[1].lng").value(-78.48))
                // Origen = el configurado en application-test.yml.
                .andExpect(jsonPath("$.origenLat").value(-0.180653))
                .andExpect(jsonPath("$.origenLng").value(-78.467834));
    }

    @Test
    void misEntregasIncluyeCoordenadasDeParadaYOrigen() throws Exception {
        String admin = tokenAdmin();
        Repartidor r = repartidorConToken();
        long asignacionId = asignar(admin, pedidoDomicilioListo(), r.id);
        long recorridoId = crearRecorrido(r.token, "INDIVIDUAL", List.of(asignacionId));

        MvcResult res = mockMvc.perform(get("/api/v1/delivery/mis-entregas")
                        .header("Authorization", "Bearer " + r.token))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode root = objectMapper.readTree(res.getResponse().getContentAsString());
        JsonNode recorrido = null;
        for (JsonNode rec : root.path("recorridos")) {
            if (rec.path("recorridoId").asLong() == recorridoId) {
                recorrido = rec;
            }
        }
        org.junit.jupiter.api.Assertions.assertNotNull(recorrido, "recorrido no visible en mis-entregas");
        // Origen del restaurante (configurado) presente en el recorrido.
        org.junit.jupiter.api.Assertions.assertEquals(0,
                new java.math.BigDecimal("-0.180653").compareTo(recorrido.path("origenLat").decimalValue()));
        org.junit.jupiter.api.Assertions.assertEquals(0,
                new java.math.BigDecimal("-78.467834").compareTo(recorrido.path("origenLng").decimalValue()));
        // Coordenadas de la parada tomadas del geocoding simulado.
        JsonNode parada = recorrido.path("paradas").get(0);
        org.junit.jupiter.api.Assertions.assertEquals(0,
                new java.math.BigDecimal("-0.19").compareTo(parada.path("lat").decimalValue()));
        org.junit.jupiter.api.Assertions.assertEquals(0,
                new java.math.BigDecimal("-78.48").compareTo(parada.path("lng").decimalValue()));
    }

    @Test
    void recalcularExcluyeResueltasYReusaCacheSiClaveCoincide() throws Exception {
        String admin = tokenAdmin();
        Repartidor r = repartidorConToken();
        long a1 = asignar(admin, pedidoDomicilioListo(), r.id);
        long a2 = asignar(admin, pedidoDomicilioListo(), r.id);
        long recorridoId = crearRecorrido(r.token, "MULTIPLE", List.of(a1, a2));
        iniciar(r.token, recorridoId);

        // Resuelve la primera parada: debe quedar excluida de los recálculos.
        List<Long> todas = paradas(r.token, recorridoId);
        entregar(r.token, todas.get(0), "k-resuelta").andExpect(status().isOk());

        // Llamadas a Directions realizadas hasta ahora (crear/iniciar no las usan,
        // pero el flujo previo pudo cachear). Contamos desde aquí.
        Mockito.clearInvocations(orsClient);

        String cuerpo = """
                {"lat":-0.1850,"lng":-78.4700}
                """;
        // Primer recalculo: calcula y cachea la ruta de las paradas PENDIENTES.
        mockMvc.perform(post("/api/v1/delivery/recorridos/{id}/recalcular", recorridoId)
                        .header("Authorization", "Bearer " + r.token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.geometria").value("geometria-fake"));
        // Segundo recalculo con la MISMA ubicación/paradas: clave de caché idéntica
        // -> NO vuelve a llamar a ORS Directions.
        mockMvc.perform(post("/api/v1/delivery/recorridos/{id}/recalcular", recorridoId)
                        .header("Authorization", "Bearer " + r.token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isOk());

        // Exactamente una llamada real a Directions en los dos recálculos.
        Mockito.verify(orsClient, Mockito.times(1)).calcularRuta(any(), any());
    }

    // ---------- PROPIEDAD / GUARD / 503 ----------

    @Test
    void repartidorNoPuedeOperarRecursoDeOtro() throws Exception {
        String admin = tokenAdmin();
        Repartidor a = repartidorConToken();
        Repartidor b = repartidorConToken();
        long asignacionId = asignar(admin, pedidoDomicilioListo(), a.id);

        // B intenta crear recorrido con la asignación de A -> 403.
        mockMvc.perform(post("/api/v1/delivery/recorridos")
                        .header("Authorization", "Bearer " + b.token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tipo":"INDIVIDUAL","asignacionIds":[%d]}
                                """.formatted(asignacionId)))
                .andExpect(status().isForbidden());
    }

    @Test
    void repartidorSuspendidoNoOpera() throws Exception {
        String admin = tokenAdmin();
        Repartidor r = repartidorConToken();
        // Suspender al repartidor: pierde la operación aunque tenga el permiso.
        mockMvc.perform(post("/api/v1/delivery/repartidores/{usuarioId}/suspender", r.usuarioId)
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/delivery/mis-entregas")
                        .header("Authorization", "Bearer " + r.token))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.type").value("urn:problem:restaurante:regla-de-negocio"));
    }

    @Test
    void orsCaidoDevuelve503() throws Exception {
        String admin = tokenAdmin();
        Repartidor r = repartidorConToken();
        long asignacionId = asignar(admin, pedidoDomicilioListo(), r.id);
        // ORS devuelve coordenada (geocoding) pero Directions falla.
        Mockito.when(orsClient.calcularRuta(any(), any()))
                .thenThrow(new com.restaurante.delivery.domain.RutasNoDisponiblesException("ORS caído"));

        mockMvc.perform(post("/api/v1/delivery/rutas/individual")
                        .header("Authorization", "Bearer " + r.token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"asignacionId":%d}
                                """.formatted(asignacionId)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.type").value("urn:problem:restaurante:rutas-no-disponibles"));
    }

    // ================= helpers =================

    private record Repartidor(long usuarioId, long id, String token) {
    }

    /** Registra un repartidor, lo habilita, verifica su correo y devuelve su
     *  token + ids. */
    private Repartidor repartidorConToken() throws Exception {
        String admin = tokenAdmin();
        long semilla = System.nanoTime();
        String correo = "repartidor_fase5_" + Math.abs(semilla) + "@test.com";
        String celular = SoporteRegistro.celular(semilla);
        String password = "Clave#123";
        mockMvc.perform(post("/api/v1/auth/registro-repartidor")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","nombre":"Repartidor Fase5","celular":"%s","password":"%s"}
                                """.formatted(correo, celular, password)))
                .andExpect(status().isCreated());
        long usuarioId = usuarioRepository.findByUsername(correo).orElseThrow().getId();
        // Verifica el correo directo (el flujo de verificación por email no es lo
        // que prueba esta clase) para poder iniciar sesión.
        Usuario usuario = usuarioRepository.findByUsername(correo).orElseThrow();
        usuario.marcarEmailVerificado();
        usuarioRepository.save(usuario);
        // Habilitar como repartidor.
        mockMvc.perform(post("/api/v1/delivery/repartidores/{usuarioId}/habilitar", usuarioId)
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk());
        long repartidorId = repartidorIdPorUsuario(admin, usuarioId);
        String token = loginToken(correo, password);
        return new Repartidor(usuarioId, repartidorId, token);
    }

    private long repartidorIdPorUsuario(String admin, long usuarioId) throws Exception {
        MvcResult res = mockMvc.perform(get("/api/v1/delivery/repartidores")
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andReturn();
        for (JsonNode n : objectMapper.readTree(res.getResponse().getContentAsString())) {
            if (n.path("usuarioId").asLong() == usuarioId) {
                return n.path("id").asLong();
            }
        }
        throw new IllegalStateException("No se encontró el repartidor del usuario " + usuarioId);
    }

    /** Asigna un pedido a un repartidor (admin) y devuelve el asignacionId. */
    private long asignar(String admin, long pedidoId, long repartidorId) throws Exception {
        MvcResult res = mockMvc.perform(post("/api/v1/delivery/asignaciones")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"pedidoClienteId":%d,"repartidorId":%d}
                                """.formatted(pedidoId, repartidorId)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(res.getResponse().getContentAsString()).path("asignacionId").asLong();
    }

    private long crearRecorrido(String token, String tipo, List<Long> asignacionIds) throws Exception {
        String ids = asignacionIds.stream().map(String::valueOf)
                .collect(java.util.stream.Collectors.joining(","));
        MvcResult res = mockMvc.perform(post("/api/v1/delivery/recorridos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tipo":"%s","asignacionIds":[%s]}
                                """.formatted(tipo, ids)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(res.getResponse().getContentAsString()).path("recorridoId").asLong();
    }

    private void iniciar(String token, long recorridoId) throws Exception {
        mockMvc.perform(post("/api/v1/delivery/recorridos/{id}/iniciar", recorridoId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    private ResultActions entregar(String token, long paradaId, String key) throws Exception {
        return mockMvc.perform(post("/api/v1/delivery/paradas/{id}/entregada", paradaId)
                .header("Authorization", "Bearer " + token)
                .header("Idempotency-Key", key));
    }

    private ResultActions incidencia(String token, long paradaId, String motivo) throws Exception {
        return mockMvc.perform(post("/api/v1/delivery/paradas/{id}/incidencia", paradaId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"motivo":"%s"}
                        """.formatted(motivo)));
    }

    private long primeraParada(String token, long recorridoId) throws Exception {
        return paradas(token, recorridoId).get(0);
    }

    private List<Long> paradas(String token, long recorridoId) throws Exception {
        MvcResult res = mockMvc.perform(get("/api/v1/delivery/mis-entregas")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode root = objectMapper.readTree(res.getResponse().getContentAsString());
        java.util.List<Long> ids = new java.util.ArrayList<>();
        for (JsonNode rec : root.path("recorridos")) {
            if (rec.path("recorridoId").asLong() == recorridoId) {
                for (JsonNode p : rec.path("paradas")) {
                    ids.add(p.path("paradaId").asLong());
                }
            }
        }
        if (ids.isEmpty()) {
            throw new IllegalStateException("El recorrido " + recorridoId + " no tiene paradas visibles");
        }
        return ids;
    }

    private String estadoPedido(long pedidoId) {
        return pedidoClienteRepository.findById(pedidoId).orElseThrow().getEstado().name();
    }

    // ---- pedido a domicilio LISTO (reusa el flujo de clientes) ----

    private long pedidoDomicilioListo() throws Exception {
        String cliente = tokenCliente("cliente_fase5", "claveCli123");
        String admin = tokenAdmin();
        long productoId = crearProducto(admin, "ProdF5-" + System.nanoTime(), "12.00");
        long direccionId = crearDireccion(cliente);
        String codigo = "CLI-F5-" + System.nanoTime();
        String idem = "idem-f5-" + System.nanoTime();
        mockMvc.perform(post("/api/v1/clientes/pedidos")
                        .header("Authorization", "Bearer " + cliente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"codigo":"%s","metodoPago":"EFECTIVO","metodoEntrega":"DOMICILIO",
                                 "direccionId":%d,"idempotencyKey":"%s",
                                 "items":[{"productoId":%d,"cantidad":1,"extraIds":[],"ingredientesRemovidos":[]}]}
                                """.formatted(codigo, direccionId, idem, productoId)))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/clientes/pedidos/" + codigo + "/confirmar")
                        .header("Authorization", "Bearer " + cliente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idempotencyKey":"%s"}
                                """.formatted(idem)))
                .andExpect(status().isOk());
        String cocina = tokenEmpleadoCocina("cocina_f5");
        mockMvc.perform(post("/api/v1/clientes/pedidos/" + codigo + "/en-preparacion")
                        .header("Authorization", "Bearer " + cocina))
                .andExpect(status().isOk());
        String mesero = tokenMesero();
        mockMvc.perform(post("/api/v1/clientes/pedidos/" + codigo + "/listo")
                        .header("Authorization", "Bearer " + mesero))
                .andExpect(status().isOk());
        return pedidoClienteRepository.findByCodigo(codigo).orElseThrow().getId();
    }

    private long crearDireccion(String clienteToken) throws Exception {
        MvcResult dirRes = mockMvc.perform(post("/api/v1/clientes/direcciones")
                        .header("Authorization", "Bearer " + clienteToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"etiqueta":"Casa-%d","direccion":"Av. Siempre Viva 742","telefono":"0999999999","observaciones":"azul"}
                                """.formatted(System.nanoTime())))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(dirRes.getResponse().getContentAsString()).path("id").asLong();
    }

    private long crearProducto(String admin, String nombre, String precio) throws Exception {
        String body = mockMvc.perform(post("/api/v1/productos")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"%s","precio":%s,"extraIds":[],"ingredientes":["cebolla"]}
                                """.formatted(nombre, precio)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).path("id").asLong();
    }
}
