package com.restaurante.delivery;

import com.restaurante.AbstractIntegracionApi;
import com.restaurante.SoporteRegistro;
import com.restaurante.usuarios.EmisorCorreo;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Fase 3 del módulo delivery: el ADMIN asigna y reasigna pedidos a domicilio a
 * repartidores habilitados, y consulta los estados de reparto. Se valida el
 * estado logístico (independiente del de preparación), las reglas de negocio
 * (habilitado, DOMICILIO/LISTO, unicidad de asignación activa) y el permiso.
 */
class DeliveryAsignacionesIntegrationTest extends AbstractIntegracionApi {

    @MockitoBean
    private EmisorCorreo emisorCorreo;

    @Test
    void asignarPedidoListoDevuelve201YQuedaAsignada() throws Exception {
        String admin = tokenAdmin();
        long repartidorId = repartidorHabilitado();
        long pedidoId = pedidoDomicilioListo();

        asignar(admin, pedidoId, repartidorId)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.pedidoClienteId").value(pedidoId))
                .andExpect(jsonPath("$.repartidorId").value(repartidorId))
                .andExpect(jsonPath("$.estado").value("ASIGNADA"))
                .andExpect(jsonPath("$.direccion").value("Calle 123"));

        // Ya no aparece como pendiente de asignar.
        MvcResult pendientes = mockMvc.perform(get("/api/v1/delivery/asignaciones/pendientes-de-asignar")
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andReturn();
        assertTrue(noContienePedido(pendientes, pedidoId));

        // Aparece en los estados de reparto.
        mockMvc.perform(get("/api/v1/delivery/asignaciones")
                        .param("repartidorId", String.valueOf(repartidorId))
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.pedidoClienteId == " + pedidoId + ")].estado").value(
                        org.hamcrest.Matchers.hasItem("ASIGNADA")));
    }

    @Test
    void asignarARepartidorNoHabilitadoDa422() throws Exception {
        String admin = tokenAdmin();
        long repartidorId = repartidorRegistradoNoHabilitado();
        long pedidoId = pedidoDomicilioListo();

        asignar(admin, pedidoId, repartidorId)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.type").value("urn:problem:restaurante:regla-de-negocio"));
    }

    @Test
    void asignarPedidoNoListoDa422() throws Exception {
        String admin = tokenAdmin();
        long repartidorId = repartidorHabilitado();
        // DOMICILIO pero solo CONFIRMADO (no LISTO): no es candidato.
        String codigo = crearPedidoDomicilio();
        confirmar(codigo);
        long pedidoId = idDePedidoConfirmadoDomicilio(admin, codigo);

        asignar(admin, pedidoId, repartidorId)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.type").value("urn:problem:restaurante:regla-de-negocio"));
    }

    @Test
    void asignarPedidoConAsignacionActivaDa422() throws Exception {
        String admin = tokenAdmin();
        long repartidorId = repartidorHabilitado();
        long pedidoId = pedidoDomicilioListo();

        asignar(admin, pedidoId, repartidorId).andExpect(status().isCreated());
        // Segunda asignación del mismo pedido: ya tiene una activa.
        asignar(admin, pedidoId, repartidorId)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.type").value("urn:problem:restaurante:regla-de-negocio"));
    }

    @Test
    void reasignarDejaViejaReasignadaYNuevaAsignada() throws Exception {
        String admin = tokenAdmin();
        long repartidorA = repartidorHabilitado();
        long repartidorB = repartidorHabilitado();
        long pedidoId = pedidoDomicilioListo();

        asignar(admin, pedidoId, repartidorA).andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/delivery/asignaciones/{pedidoId}/reasignar", pedidoId)
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"repartidorId":%d}
                                """.formatted(repartidorB)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.repartidorId").value(repartidorB))
                .andExpect(jsonPath("$.estado").value("ASIGNADA"));

        // La vieja asignación del repartidor A quedó REASIGNADA.
        mockMvc.perform(get("/api/v1/delivery/asignaciones")
                        .param("estado", "REASIGNADA")
                        .param("repartidorId", String.valueOf(repartidorA))
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.pedidoClienteId == " + pedidoId + ")].estado").value(
                        org.hamcrest.Matchers.hasItem("REASIGNADA")));

        // Solo una activa: la nueva en el repartidor B.
        mockMvc.perform(get("/api/v1/delivery/asignaciones")
                        .param("estado", "ASIGNADA")
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.pedidoClienteId == " + pedidoId + ")].repartidorId").value(
                        org.hamcrest.Matchers.hasItem((int) repartidorB)));
    }

    @Test
    void asignarLoteAsignaTodosAlMismoRepartidor() throws Exception {
        String admin = tokenAdmin();
        long repartidorId = repartidorHabilitado();
        long pedido1 = pedidoDomicilioListo();
        long pedido2 = pedidoDomicilioListo();

        mockMvc.perform(post("/api/v1/delivery/asignaciones/lote")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"pedidoClienteIds":[%d,%d],"repartidorId":%d}
                                """.formatted(pedido1, pedido2, repartidorId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].estado").value("ASIGNADA"))
                .andExpect(jsonPath("$[1].estado").value("ASIGNADA"));
    }

    @Test
    void usuarioSinPermisoRecibe403() throws Exception {
        String mesero = tokenMesero();

        mockMvc.perform(get("/api/v1/delivery/asignaciones")
                        .header("Authorization", "Bearer " + mesero))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type").value("urn:problem:restaurante:acceso-denegado"));
    }

    // ---- helpers de asignación ----

    private org.springframework.test.web.servlet.ResultActions asignar(String admin, long pedidoId,
                                                                        long repartidorId) throws Exception {
        return mockMvc.perform(post("/api/v1/delivery/asignaciones")
                .header("Authorization", "Bearer " + admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"pedidoClienteId":%d,"repartidorId":%d}
                        """.formatted(pedidoId, repartidorId)));
    }

    /** Busca en los pendientes-de-asignar el id del pedido por su código. */
    private long pedidoDomicilioListo() throws Exception {
        String codigo = crearPedidoDomicilio();
        confirmar(codigo);
        marcarEnPreparacion(codigo);
        marcarListo(codigo);
        return idDePedidoPendiente(tokenAdmin(), codigo);
    }

    private long idDePedidoPendiente(String admin, String codigo) throws Exception {
        MvcResult res = mockMvc.perform(get("/api/v1/delivery/asignaciones/pendientes-de-asignar")
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andReturn();
        return buscarIdPorCodigo(res, codigo);
    }

    /** El pedido CONFIRMADO no aparece en pendientes (no está LISTO); su id
     *  interno se resuelve por el repositorio de clientes solo para la prueba. */
    private long idDePedidoConfirmadoDomicilio(String admin, String codigo) {
        return pedidoClienteRepository.findByCodigo(codigo).orElseThrow().getId();
    }

    // ---- helpers de datos del pedido ----

    private String crearPedidoDomicilio() throws Exception {
        String cliente = tokenCliente("cliente_delivery_asig", "claveCli123");
        String admin = tokenAdmin();
        long productoId = crearProducto(admin, "ProdAsig-" + System.nanoTime(), "12.00");
        long direccionId = crearDireccion(cliente);
        String codigo = "CLI-ASG-" + System.nanoTime();
        String idem = "idem-asg-" + System.nanoTime();
        mockMvc.perform(post("/api/v1/clientes/pedidos")
                        .header("Authorization", "Bearer " + cliente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"codigo":"%s","metodoPago":"EFECTIVO","metodoEntrega":"DOMICILIO",
                                 "direccionId":%d,"idempotencyKey":"%s",
                                 "items":[{"productoId":%d,"cantidad":1,"extraIds":[],"ingredientesRemovidos":[]}]}
                                """.formatted(codigo, direccionId, idem, productoId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metodoEntrega").value("DOMICILIO"));
        this.ultimaClaveIdem = idem;
        return codigo;
    }

    private String ultimaClaveIdem;

    private void confirmar(String codigo) throws Exception {
        String cliente = tokenCliente("cliente_delivery_asig", "claveCli123");
        mockMvc.perform(post("/api/v1/clientes/pedidos/" + codigo + "/confirmar")
                        .header("Authorization", "Bearer " + cliente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idempotencyKey":"%s"}
                                """.formatted(ultimaClaveIdem)))
                .andExpect(status().isOk());
    }

    private void marcarEnPreparacion(String codigo) throws Exception {
        String cocina = tokenEmpleadoCocina("cocina_asig");
        mockMvc.perform(post("/api/v1/clientes/pedidos/" + codigo + "/en-preparacion")
                        .header("Authorization", "Bearer " + cocina))
                .andExpect(status().isOk());
    }

    private void marcarListo(String codigo) throws Exception {
        String mesero = tokenMesero();
        mockMvc.perform(post("/api/v1/clientes/pedidos/" + codigo + "/listo")
                        .header("Authorization", "Bearer " + mesero))
                .andExpect(status().isOk());
    }

    private long crearDireccion(String clienteToken) throws Exception {
        MvcResult dirRes = mockMvc.perform(post("/api/v1/clientes/direcciones")
                        .header("Authorization", "Bearer " + clienteToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"etiqueta":"Casa-%d","direccion":"Calle 123","telefono":"0999999999","observaciones":"azul"}
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

    // ---- helpers de repartidor ----

    private long repartidorHabilitado() throws Exception {
        String admin = tokenAdmin();
        long usuarioId = registrarRepartidor();
        mockMvc.perform(post("/api/v1/delivery/repartidores/{usuarioId}/habilitar", usuarioId)
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk());
        return repartidorIdPorUsuario(admin, usuarioId);
    }

    private long repartidorRegistradoNoHabilitado() throws Exception {
        String admin = tokenAdmin();
        long usuarioId = registrarRepartidor();
        // Habilitar y suspender: queda registrado (id) pero no HABILITADO.
        mockMvc.perform(post("/api/v1/delivery/repartidores/{usuarioId}/habilitar", usuarioId)
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk());
        long id = repartidorIdPorUsuario(admin, usuarioId);
        mockMvc.perform(post("/api/v1/delivery/repartidores/{usuarioId}/suspender", usuarioId)
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk());
        return id;
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

    private Long registrarRepartidor() throws Exception {
        long semilla = System.nanoTime();
        String correo = "repartidor_asig_" + Math.abs(semilla) + "@test.com";
        String celular = SoporteRegistro.celular(semilla);
        mockMvc.perform(post("/api/v1/auth/registro-repartidor")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","nombre":"Repartidor Fase3","celular":"%s","password":"Clave#123"}
                                """.formatted(correo, celular)))
                .andExpect(status().isCreated());
        return usuarioRepository.findByUsername(correo).orElseThrow().getId();
    }

    // ---- utilidades ----

    private long buscarIdPorCodigo(MvcResult res, String codigo) throws Exception {
        for (JsonNode n : objectMapper.readTree(res.getResponse().getContentAsString())) {
            if (codigo.equals(n.path("codigo").asText())) {
                return n.path("pedidoClienteId").asLong();
            }
        }
        throw new IllegalStateException("No se encontró el pedido " + codigo + " en la respuesta");
    }

    private boolean noContienePedido(MvcResult res, long pedidoId) throws Exception {
        for (JsonNode n : objectMapper.readTree(res.getResponse().getContentAsString())) {
            if (n.path("pedidoClienteId").asLong() == pedidoId) {
                return false;
            }
        }
        return true;
    }

    @org.springframework.beans.factory.annotation.Autowired
    private com.restaurante.clientes.infrastructure.PedidoClienteRepository pedidoClienteRepository;
}
