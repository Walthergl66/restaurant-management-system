package com.restaurante.delivery;

import com.restaurante.AbstractIntegracionApi;
import com.restaurante.SoporteRegistro;
import com.restaurante.delivery.domain.Coordenada;
import com.restaurante.delivery.domain.ResultadoRuta;
import com.restaurante.delivery.infrastructure.OrsClient;
import com.restaurante.usuarios.EmisorCorreo;
import com.restaurante.usuarios.domain.Usuario;
import org.junit.jupiter.api.Assertions;
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
 * Fase 2 del módulo delivery: DOBLE CONFIRMACIÓN de entrega (repartidor +
 * cliente). Valida que los cuatro conceptos quedan separados (confirmación del
 * repartidor, confirmación del cliente, estado logístico y estado final
 * verificado), la propiedad del pedido para el cliente, la idempotencia, que el
 * recorrido puede finalizar aunque el cliente no confirme, la resolución
 * administrativa auditada y que cada parada de una multientrega lleva su propia
 * confirmación. ORS está MOCKEADO.
 */
class DeliveryDobleConfirmacionIntegrationTest extends AbstractIntegracionApi {

    @MockitoBean
    private EmisorCorreo emisorCorreo;

    @MockitoBean
    private OrsClient orsClient;

    @Autowired
    private com.restaurante.clientes.infrastructure.PedidoClienteRepository pedidoClienteRepository;

    @Autowired
    private com.restaurante.delivery.application.ConfigRestauranteService configRestaurante;

    @BeforeEach
    void stubOrs() {
        Mockito.reset(orsClient);
        Mockito.when(orsClient.geocodeDireccion(anyString()))
                .thenReturn(Optional.of(Coordenada.of(-0.19, -78.48)));
        configRestaurante.fijarCoordenadasManual(
                java.math.BigDecimal.valueOf(-0.180653),
                java.math.BigDecimal.valueOf(-78.467834), "test");
        Mockito.when(orsClient.calcularRuta(any(), any()))
                .thenReturn(new ResultadoRuta("geometria-fake", 1500, 600));
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

    @Test
    void repartidorConfirmaDejaParadaEntregadaYPendienteConfirmacionCliente() throws Exception {
        String admin = tokenAdmin();
        Repartidor r = repartidorConToken();
        Pedido p = pedidoDomicilioListo();
        String estadoPrepAntes = estadoPedido(p.id);
        long asignacionId = asignar(admin, p.id, r.id);
        long recorridoId = crearRecorrido(r.token, "INDIVIDUAL", List.of(asignacionId));
        long paradaId = primeraParada(r.token, recorridoId);
        iniciar(r.token, recorridoId);

        entregar(r.token, paradaId, "k-1").andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ENTREGADA"));

        // Fase de confirmación: pendiente del cliente; asignación ENTREGADA; NO verificada.
        MvcResult seg = mockMvc.perform(get("/api/v1/clientes/pedidos/{c}/seguimiento", p.codigo)
                        .header("Authorization", "Bearer " + p.token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.faseConfirmacion").value("PENDIENTE_CONFIRMACION_CLIENTE"))
                .andExpect(jsonPath("$.faseLogistica").value("ENTREGADO_POR_REPARTIDOR"))
                .andExpect(jsonPath("$.entregaVerificada").value(false))
                .andReturn();
        Assertions.assertNotNull(seg);

        // El estado de PREPARACIÓN del pedido NO cambió por la confirmación.
        Assertions.assertEquals(estadoPrepAntes, estadoPedido(p.id));
    }

    @Test
    void clienteConfirmaMarcaVerificada() throws Exception {
        String admin = tokenAdmin();
        Repartidor r = repartidorConToken();
        Pedido p = pedidoDomicilioListo();
        long asignacionId = asignar(admin, p.id, r.id);
        long recorridoId = crearRecorrido(r.token, "INDIVIDUAL", List.of(asignacionId));
        long paradaId = primeraParada(r.token, recorridoId);
        iniciar(r.token, recorridoId);
        entregar(r.token, paradaId, "k-1").andExpect(status().isOk());

        confirmarRecepcion(p.token, p.codigo, "c-1").andExpect(status().isOk())
                .andExpect(jsonPath("$.faseConfirmacion").value("VERIFICADA"))
                .andExpect(jsonPath("$.entregaVerificada").value(true));
    }

    @Test
    void clienteNoPuedeConfirmarAntesDeQueRepartidorEntregue() throws Exception {
        String admin = tokenAdmin();
        Repartidor r = repartidorConToken();
        Pedido p = pedidoDomicilioListo();
        long asignacionId = asignar(admin, p.id, r.id);
        long recorridoId = crearRecorrido(r.token, "INDIVIDUAL", List.of(asignacionId));
        iniciar(r.token, recorridoId);

        confirmarRecepcion(p.token, p.codigo, "c-1")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.type").value("urn:problem:restaurante:regla-de-negocio"));
    }

    @Test
    void clienteNoPuedeConfirmarPedidoAjeno() throws Exception {
        String admin = tokenAdmin();
        Repartidor r = repartidorConToken();
        Pedido p = pedidoDomicilioListo();
        long asignacionId = asignar(admin, p.id, r.id);
        long recorridoId = crearRecorrido(r.token, "INDIVIDUAL", List.of(asignacionId));
        long paradaId = primeraParada(r.token, recorridoId);
        iniciar(r.token, recorridoId);
        entregar(r.token, paradaId, "k-1").andExpect(status().isOk());

        // Otro cliente intenta confirmar: 404 (no se revela el pedido ajeno).
        String ajeno = tokenCliente("cliente_ajeno_f2", "claveAjeno123");
        confirmarRecepcion(ajeno, p.codigo, "c-x")
                .andExpect(status().isNotFound());
    }

    @Test
    void confirmacionClienteEsIdempotente() throws Exception {
        String admin = tokenAdmin();
        Repartidor r = repartidorConToken();
        Pedido p = pedidoDomicilioListo();
        long asignacionId = asignar(admin, p.id, r.id);
        long recorridoId = crearRecorrido(r.token, "INDIVIDUAL", List.of(asignacionId));
        long paradaId = primeraParada(r.token, recorridoId);
        iniciar(r.token, recorridoId);
        entregar(r.token, paradaId, "k-1").andExpect(status().isOk());

        confirmarRecepcion(p.token, p.codigo, "c-1").andExpect(status().isOk())
                .andExpect(jsonPath("$.clienteConfirmoAt").isNotEmpty());
        MvcResult primera = mockMvc.perform(get("/api/v1/clientes/pedidos/{c}/seguimiento", p.codigo)
                        .header("Authorization", "Bearer " + p.token))
                .andExpect(status().isOk()).andReturn();
        String tsPrimera = objectMapper.readTree(primera.getResponse().getContentAsString())
                .path("clienteConfirmoAt").asText();

        // Segunda confirmación (misma key): sigue verificada, no duplica la fecha.
        confirmarRecepcion(p.token, p.codigo, "c-1").andExpect(status().isOk())
                .andExpect(jsonPath("$.entregaVerificada").value(true));
        MvcResult segunda = mockMvc.perform(get("/api/v1/clientes/pedidos/{c}/seguimiento", p.codigo)
                        .header("Authorization", "Bearer " + p.token))
                .andExpect(status().isOk()).andReturn();
        String tsSegunda = objectMapper.readTree(segunda.getResponse().getContentAsString())
                .path("clienteConfirmoAt").asText();
        Assertions.assertEquals(tsPrimera, tsSegunda, "la confirmación del cliente no debe duplicarse");
    }

    @Test
    void finalizarRecorridoPermitidoConClientePendiente() throws Exception {
        String admin = tokenAdmin();
        Repartidor r = repartidorConToken();
        Pedido p = pedidoDomicilioListo();
        long asignacionId = asignar(admin, p.id, r.id);
        long recorridoId = crearRecorrido(r.token, "INDIVIDUAL", List.of(asignacionId));
        long paradaId = primeraParada(r.token, recorridoId);
        iniciar(r.token, recorridoId);
        entregar(r.token, paradaId, "k-1").andExpect(status().isOk());

        // Cliente NO confirma: el recorrido igual puede finalizar (no bloquea).
        mockMvc.perform(post("/api/v1/delivery/recorridos/{id}/finalizar", recorridoId)
                        .header("Authorization", "Bearer " + r.token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("FINALIZADO"));
    }

    @Test
    void adminListaPendientesYResuelveConMotivo() throws Exception {
        String admin = tokenAdmin();
        Repartidor r = repartidorConToken();
        Pedido p = pedidoDomicilioListo();
        long asignacionId = asignar(admin, p.id, r.id);
        long recorridoId = crearRecorrido(r.token, "INDIVIDUAL", List.of(asignacionId));
        long paradaId = primeraParada(r.token, recorridoId);
        iniciar(r.token, recorridoId);
        entregar(r.token, paradaId, "k-1").andExpect(status().isOk());

        // Aparece en pendientes.
        mockMvc.perform(get("/api/v1/delivery/confirmaciones/pendientes")
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.paradaId == " + paradaId + ")].faseConfirmacion").value(
                        org.hamcrest.Matchers.hasItem("PENDIENTE_CONFIRMACION_CLIENTE")));

        // Resuelve con motivo -> verificada.
        mockMvc.perform(post("/api/v1/delivery/confirmaciones/{id}/resolver", paradaId)
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"motivo":"Cliente no responde; entrega confirmada por foto"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entregaVerificada").value(true))
                .andExpect(jsonPath("$.faseConfirmacion").value("VERIFICADA"));
    }

    @Test
    void multientregaCadaParadaMantieneSuConfirmacionIndependiente() throws Exception {
        String admin = tokenAdmin();
        Repartidor r = repartidorConToken();
        Pedido p1 = pedidoDomicilioListo();
        Pedido p2 = pedidoDomicilioListo();
        long a1 = asignar(admin, p1.id, r.id);
        long a2 = asignar(admin, p2.id, r.id);
        long recorridoId = crearRecorrido(r.token, "MULTIPLE", List.of(a1, a2));
        iniciar(r.token, recorridoId);

        // El repartidor confirma AMBAS paradas.
        for (long paradaId : paradas(r.token, recorridoId)) {
            entregar(r.token, paradaId, "k-" + paradaId).andExpect(status().isOk());
        }

        // Solo el cliente de p1 confirma: su entrega queda verificada y la de p2 no.
        confirmarRecepcion(p1.token, p1.codigo, "c-1").andExpect(status().isOk())
                .andExpect(jsonPath("$.entregaVerificada").value(true));

        mockMvc.perform(get("/api/v1/clientes/pedidos/{c}/seguimiento", p2.codigo)
                        .header("Authorization", "Bearer " + p2.token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.faseConfirmacion").value("PENDIENTE_CONFIRMACION_CLIENTE"))
                .andExpect(jsonPath("$.entregaVerificada").value(false));
    }

    // ================= helpers =================

    private record Repartidor(long usuarioId, long id, String token) {
    }

    private record Pedido(long id, String codigo, String token) {
    }

    private Repartidor repartidorConToken() throws Exception {
        String admin = tokenAdmin();
        long semilla = System.nanoTime();
        String correo = "repartidor_f2_" + Math.abs(semilla) + "@test.com";
        String celular = SoporteRegistro.celular(semilla);
        String password = "Clave#123";
        mockMvc.perform(post("/api/v1/auth/registro-repartidor")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","nombre":"Repartidor F2","celular":"%s","password":"%s"}
                                """.formatted(correo, celular, password)))
                .andExpect(status().isCreated());
        long usuarioId = usuarioRepository.findByUsername(correo).orElseThrow().getId();
        Usuario usuario = usuarioRepository.findByUsername(correo).orElseThrow();
        usuario.marcarEmailVerificado();
        usuarioRepository.save(usuario);
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

    private ResultActions confirmarRecepcion(String token, String codigo, String key) throws Exception {
        return mockMvc.perform(post("/api/v1/clientes/pedidos/{c}/confirmar-recepcion", codigo)
                .header("Authorization", "Bearer " + token)
                .header("Idempotency-Key", key));
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
                for (JsonNode pp : rec.path("paradas")) {
                    ids.add(pp.path("paradaId").asLong());
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

    /** Crea un pedido a domicilio LISTO y devuelve su id, código y el token del
     *  cliente DUEÑO (para la confirmación del cliente). */
    private Pedido pedidoDomicilioListo() throws Exception {
        String cliente = tokenCliente("cliente_f2", "claveCli123");
        String admin = tokenAdmin();
        long productoId = crearProducto(admin, "ProdF2-" + System.nanoTime(), "12.00");
        long direccionId = crearDireccion(cliente);
        String codigo = "CLI-F2-" + System.nanoTime();
        String idem = "idem-f2-" + System.nanoTime();
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
        String cocina = tokenEmpleadoCocina("cocina_f2");
        mockMvc.perform(post("/api/v1/clientes/pedidos/" + codigo + "/en-preparacion")
                        .header("Authorization", "Bearer " + cocina))
                .andExpect(status().isOk());
        String mesero = tokenMesero();
        mockMvc.perform(post("/api/v1/clientes/pedidos/" + codigo + "/listo")
                        .header("Authorization", "Bearer " + mesero))
                .andExpect(status().isOk());
        long id = pedidoClienteRepository.findByCodigo(codigo).orElseThrow().getId();
        return new Pedido(id, codigo, cliente);
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
