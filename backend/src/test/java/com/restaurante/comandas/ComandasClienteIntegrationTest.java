package com.restaurante.comandas;

import com.restaurante.AbstractIntegracionApi;
import com.restaurante.clientes.PedidoClienteEventos;
import com.restaurante.shared.outbox.EstadoOutbox;
import com.restaurante.shared.outbox.OutboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;

import java.util.Arrays;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A-08: al confirmar un pedido de la app del cliente, un listener genera en la
 * MISMA transacción una comanda por área y su orden de impresión en el outbox,
 * visible en cocina con el código del cliente. Idempotente ante reintentos y
 * sin comanda cuando ningún producto tiene área.
 */
class ComandasClienteIntegrationTest extends AbstractIntegracionApi {

    private String administradorToken;
    private String cocineroToken;
    private String clienteToken;

    @Autowired
    private OutboxRepository outboxRepository;

    @BeforeEach
    void preparar() {
        administradorToken = tokenAdmin();
        cocineroToken = tokenEmpleadoCocina("cocina_clicom");
        clienteToken = tokenCliente("cliente_anticom", "claveAnt123");
    }

    @Test
    void confirmarPedidoClienteGeneraComandaPorAreaYOrdenDeImpresion() throws Exception {
        long areaCocina = crearArea("Cocina");
        long areaBarra = crearArea("Barra");
        long productoA = crearProductoConArea("Pollo Cli", "8.50", areaCocina);
        long productoB = crearProductoConArea("Jugo Cli", "2.50", areaBarra);

        String codigo = "CLI-COM-" + System.nanoTime();
        String idem = "idem-clicom-" + System.nanoTime();
        crearPedidoCliente(codigo, idem, productoA, productoB);
        confirmarCliente(codigo, idem);

        // Comandas: una por área y el código del cliente como identificador
        JsonNode comandas = buscar(codigo, null);
        assertEquals(2, comandas.size(), "una comanda por área");
        JsonNode cocina = buscarPorArea(comandas, areaCocina);
        assertEquals("PENDIENTE", cocina.path("estado").asText());
        assertEquals(codigo, cocina.path("pedidoCodigo").asText());
        assertEquals(1, cocina.path("lineas").size());
        assertTrue(cocina.path("lineas").get(0).path("nombreProducto").asText().startsWith("Pollo Cli"));
        assertEquals(2, cocina.path("lineas").get(0).path("cantidad").asInt());

        // Órdenes de impresión en el outbox
        JsonNode pendientes = buscarPendientes(codigo);
        assertEquals(2, pendientes.size(), "una orden de impresión por comanda");
        assertTrue(pendientes.toString().contains("comanda.impresion"));
        assertTrue(pendientes.toString().contains("Barra"));
    }

    @Test
    void confirmarIdempotenteNoDuplicaComandasNiOrdenes() throws Exception {
        long area = crearArea("Horno");
        long producto = crearProductoConArea("Lasagna Cli", "9.00", area);
        String codigo = "CLI-COM-" + System.nanoTime();
        String idem = "idem-clicom-" + System.nanoTime();
        crearPedidoCliente(codigo, idem, producto);
        confirmarCliente(codigo, idem);
        confirmarCliente(codigo, idem);

        assertEquals(1, buscar(codigo, null).size(), "el reintento no duplica comandas");
        assertEquals(1, buscarPendientes(codigo).size(), "ni órdenes de impresión");
    }

    @Test
    void productoSinAreaNoGeneraComanda() throws Exception {
        long producto = crearProductoSinArea("Agua Cli", "1.00");
        String codigo = "CLI-COM-" + System.nanoTime();
        String idem = "idem-clicom-" + System.nanoTime();
        crearPedidoCliente(codigo, idem, producto);
        confirmarCliente(codigo, idem);

        assertEquals(0, buscar(codigo, null).size(), "un producto sin área no genera comanda");
    }

    @Test
    void cocinaAvanzaPedidoClienteDesdeLaComanda() throws Exception {
        long area = crearArea("Parrilla");
        long producto = crearProductoConArea("Churrasco Cli", "14.00", area);
        String codigo = "CLI-COM-" + System.nanoTime();
        String idem = "idem-clicom-" + System.nanoTime();
        crearPedidoCliente(codigo, idem, producto);
        confirmarCliente(codigo, idem);

        long comandaId = buscar(codigo, null).path(0).path("id").asLong();
        mockMvc.perform(post("/api/v1/comandas/" + comandaId + "/en-preparacion")
                        .header("Authorization", "Bearer " + cocineroToken))
                .andExpect(status().isOk());

        // La comanda y el pedido del cliente avanzan en la misma operación
        JsonNode comandas = buscar(codigo, "EN_PREPARACION");
        assertEquals(1, comandas.size());

        JsonNode historial = objectMapper.readTree(mockMvc.perform(get("/api/v1/clientes/historial?page=0&size=50")
                        .header("Authorization", "Bearer " + clienteToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        JsonNode pedido = null;
        for (JsonNode n : historial.path("content")) {
            if (codigo.equals(n.path("codigo").asText())) {
                pedido = n;
            }
        }
        assertEquals("EN_PREPARACION", pedido.path("estado").asText());
    }

    /**
     * El paso de la comanda reintenta es normal: la app de cocina repite la
     * llamada si se quedó sin red a media confirmación, y el botón de la tablet
     * se puede pulsar dos veces. Reintentar no debe cobrarle al cliente su
     * pedido: el estado ya pedido tiene que aceptarse sin error, igual que
     * pasa con los pedidos presenciales.
     */
    @Test
    void repetirElPasoDeLaComandaNoFallaEnPedidoCliente() throws Exception {
        long area = crearArea("Parrilla " + System.nanoTime() % 100000);
        long producto = crearProductoConArea("Churrasco Reintento", "14.00", area);
        String codigo = "CLI-REINT-" + System.nanoTime();
        String idem = "idem-clireint-" + System.nanoTime();
        crearPedidoCliente(codigo, idem, producto);
        confirmarCliente(codigo, idem);
        long comandaId = buscar(codigo, null).path(0).path("id").asLong();

        marcar(comandaId, "en-preparacion");
        marcar(comandaId, "en-preparacion");
        marcar(comandaId, "listo");
        marcar(comandaId, "listo");

        assertEquals(1, buscar(codigo, "LISTO").size());
        assertEquals("LISTO", estadoEnHistorial(codigo));
    }

    /**
     * El outbox es una tabla compartida: además de las órdenes de impresión,
     * guarda los avisos de cambio de estado de la app del cliente. El
     * acknowledgment del agente de impresión solo puede tocar los suyos; si
     * alcanzara un aviso ajeno, el cliente dejaría de recibir la notificación
     * de su pedido.
     */
    @Test
    void elAgenteNoPuedeAcknowledgearEventosDeOtroModulo() throws Exception {
        long area = crearArea("Cocina Shared " + System.nanoTime() % 100000);
        long producto = crearProductoConArea("Plato Shared", "5.00", area);

        String codigo = "CLI-SHARED-" + System.nanoTime();
        String idem = "idem-shared-" + System.nanoTime();
        crearPedidoCliente(codigo, idem, producto);
        confirmarCliente(codigo, idem);

        // Aviso de estado que pertenece al módulo de clientes
        long avisoId = outboxRepository
                .findByTipoAndAgregadoIdAndEvento(
                        PedidoClienteEventos.TIPO_ESTADO, codigo, PedidoClienteEventos.EVENTO_ESTADO)
                .orElseThrow(() -> new AssertionError("No se generó el aviso de estado"))
                .getId();

        // El endpoint de impresión no lo alcanza
        mockMvc.perform(post("/api/v1/comandas/impresion/" + avisoId + "/enviado")
                        .header("Authorization", "Bearer " + cocineroToken))
                .andExpect(status().isNotFound());

        // Sigue pendiente para su propio difusor
        assertEquals(EstadoOutbox.PENDIENTE,
                outboxRepository.findById(avisoId).orElseThrow().getEstado());

        // Y una orden de impresión real sí se acknowledgea
        long ordenId = buscarPendientes(codigo).path(0).path("id").asLong();
        mockMvc.perform(post("/api/v1/comandas/impresion/" + ordenId + "/enviado")
                        .header("Authorization", "Bearer " + cocineroToken))
                .andExpect(status().isOk());
    }

    private void marcar(long comandaId, String transicion) throws Exception {
        mockMvc.perform(post("/api/v1/comandas/" + comandaId + "/" + transicion)
                        .header("Authorization", "Bearer " + cocineroToken))
                .andExpect(status().isOk());
    }

    private String estadoEnHistorial(String codigo) throws Exception {
        JsonNode historial = objectMapper.readTree(mockMvc.perform(get("/api/v1/clientes/historial?page=0&size=50")
                        .header("Authorization", "Bearer " + clienteToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        for (JsonNode n : historial.path("content")) {
            if (codigo.equals(n.path("codigo").asText())) {
                return n.path("estado").asText();
            }
        }
        throw new AssertionError("El pedido no aparece en el historial: " + codigo);
    }

    // ---------------------------------------------------------------
    // Utilidades de contexto de datos
    // ---------------------------------------------------------------

    @Test
    void laComandaDeUnPedidoDeLaAppNoTraeMesaYTieneCanalApp() throws Exception {
        long area = crearArea("Canal app");
        long producto = crearProductoConArea("Plato App", "6.00", area);
        String codigo = "CLI-APP-" + System.nanoTime();
        String idem = "idem-app-" + System.nanoTime();
        crearPedidoCliente(codigo, idem, producto);
        confirmarCliente(codigo, idem);

        JsonNode comanda = buscar(codigo, null).get(0);
        assertEquals("APP", comanda.path("canal").asText());
        assertTrue(comanda.path("mesaNumero").isMissingNode(), "un pedido de la app no tiene mesa (el campo null se omite del JSON)");
        assertEquals("RETIRAR", comanda.path("entrega").asText(), "el pedido de la app de este helper es para retirar");
    }

    @Test
    void laComandaDeUnPedidoADomicilioIndicaDomicilio() throws Exception {
        long area = crearArea("Domicilio comanda");
        long producto = crearProductoConArea("Plato Domicilio", "7.00", area);
        String codigo = "CLI-DOM-" + System.nanoTime();
        crearYConfirmarDomicilio(codigo, producto);

        JsonNode comanda = buscar(codigo, null).get(0);
        assertEquals("APP", comanda.path("canal").asText());
        assertEquals("DOMICILIO", comanda.path("entrega").asText());
    }

    // ---------------------------------------------------------------
    // Entrega en el mostrador (retiro): mesero o cocina, sin PIN
    // ---------------------------------------------------------------

    @Test
    void elMeseroEntregaUnRetiroListoYElCocineroTambienSinCambiarNada() throws Exception {
        String codigo = retiroListo();
        assertEquals("LISTO", estadoEnHistorial(codigo));
        String mesero = tokenMesero();

        mockMvc.perform(post("/api/v1/clientes/pedidos/" + codigo + "/entregar").header("Authorization", "Bearer " + mesero))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ENTREGADO"));
        assertEquals("ENTREGADO", estadoEnHistorial(codigo));

        String cocina = tokenCocinaRol("cocina_mostrador_" + System.nanoTime() % 1_000_000_000L);
        mockMvc.perform(post("/api/v1/clientes/pedidos/" + codigo + "/entregar").header("Authorization", "Bearer " + cocina))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ENTREGADO"));
    }

    @Test
    void unRetiroQueAunNoEstaListoNoSeEntrega() throws Exception {
        long area = crearArea("Retiro no listo");
        long producto = crearProductoConArea("Plato Retiro", "5.00", area);
        String codigo = "CLI-RET-" + System.nanoTime();
        String idem = "idem-ret-" + System.nanoTime();
        crearPedidoCliente(codigo, idem, producto);
        confirmarCliente(codigo, idem);

        mockMvc.perform(post("/api/v1/clientes/pedidos/" + codigo + "/entregar").header("Authorization", "Bearer " + tokenMesero()))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void unDomicilioNoSeEntregaEnElMostrador() throws Exception {
        long area = crearArea("Domicilio mostrador");
        long producto = crearProductoConArea("Plato Dom Mostrador", "5.00", area);
        String codigo = "CLI-DOMM-" + System.nanoTime();
        crearYConfirmarDomicilio(codigo, producto);
        long comandaId = buscar(codigo, null).path(0).path("id").asLong();
        marcar(comandaId, "en-preparacion");
        marcar(comandaId, "listo");

        mockMvc.perform(post("/api/v1/clientes/pedidos/" + codigo + "/entregar").header("Authorization", "Bearer " + tokenMesero()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").value("El domicilio lo entrega el repartidor"));
        assertEquals("LISTO", estadoEnHistorial(codigo));
    }

    @Test
    void sinElPermisoDeMostradorSeRespondeProhibido() throws Exception {
        mockMvc.perform(post("/api/v1/clientes/pedidos/CUALQUIERA/entregar").header("Authorization", "Bearer " + clienteToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/clientes/operacion/pedidos?estado=LISTO&entrega=RETIRAR").header("Authorization", "Bearer " + clienteToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void laListaDelMostradorFiltraPorEstadoYFormaDeEntrega() throws Exception {
        String retiroListo = retiroListo();
        long area = crearArea("Lista mostrador");
        long producto = crearProductoConArea("Plato Lista", "4.00", area);
        String retiroConfirmado = "CLI-RETC-" + System.nanoTime();
        String idem = "idem-retc-" + System.nanoTime();
        crearPedidoCliente(retiroConfirmado, idem, producto);
        confirmarCliente(retiroConfirmado, idem);
        String domicilioListo = "CLI-DOML-" + System.nanoTime();
        crearYConfirmarDomicilio(domicilioListo, producto);
        long comandaDom = buscar(domicilioListo, null).path(0).path("id").asLong();
        marcar(comandaDom, "en-preparacion");
        marcar(comandaDom, "listo");

        String cuerpo = mockMvc.perform(get("/api/v1/clientes/operacion/pedidos?estado=LISTO&entrega=RETIRAR")
                        .header("Authorization", "Bearer " + tokenMesero()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode lista = objectMapper.readTree(cuerpo);
        boolean encontrado = false;
        for (JsonNode pedido : lista) {
            assertEquals("LISTO", pedido.path("estado").asText());
            assertEquals("RETIRAR", pedido.path("entrega").asText());
            if (retiroListo.equals(pedido.path("codigo").asText())) {
                encontrado = true;
                assertTrue(pedido.path("lineas").size() >= 1);
                assertTrue(pedido.path("lineas").get(0).path("cantidad").asInt() >= 1);
            }
        }
        assertTrue(encontrado, "el retiro listo aparece en la lista");
        assertTrue(!cuerpo.contains(retiroConfirmado), "un retiro aún confirmado no aparece");
        assertTrue(!cuerpo.contains(domicilioListo), "un domicilio no aparece en la lista de retiros");

        mockMvc.perform(get("/api/v1/clientes/operacion/pedidos?estado=NOEXISTE&entrega=RETIRAR")
                        .header("Authorization", "Bearer " + tokenMesero()))
                .andExpect(status().isUnprocessableEntity());
    }

    /** Un pedido de retiro de la app que cocina ya dejó LISTO. */
    private String retiroListo() throws Exception {
        long area = crearArea("Retiro listo");
        long producto = crearProductoConArea("Plato Retiro Listo", "6.00", area);
        String codigo = "CLI-RETL-" + System.nanoTime();
        String idem = "idem-retl-" + System.nanoTime();
        crearPedidoCliente(codigo, idem, producto);
        confirmarCliente(codigo, idem);
        long comandaId = buscar(codigo, null).path(0).path("id").asLong();
        marcar(comandaId, "en-preparacion");
        marcar(comandaId, "listo");
        return codigo;
    }

    private void crearYConfirmarDomicilio(String codigo, long productoId) throws Exception {
        long direccionId = crearDireccion();
        String idem = "idem-dom-" + System.nanoTime();
        mockMvc.perform(post("/api/v1/clientes/pedidos")
                        .header("Authorization", "Bearer " + clienteToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "codigo":"%s",
                                  "metodoPago":"EFECTIVO",
                                  "metodoEntrega":"DOMICILIO",
                                  "direccionId":%d,
                                  "idempotencyKey":"%s",
                                  "items":[{"productoId":%d,"cantidad":1,"extraIds":[],"ingredientesRemovidos":[]}]
                                }
                                """.formatted(codigo, direccionId, idem, productoId)))
                .andExpect(status().isOk());
        confirmarCliente(codigo, idem);
    }

    private String tokenCocinaRol(String username) throws Exception {
        mockMvc.perform(post("/api/v1/usuarios")
                        .header("Authorization", "Bearer " + administradorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","nombre":"Cocina Rol","password":"clave456","rolCodigo":"COCINA"}
                                """.formatted(username)))
                .andExpect(status().isCreated());
        return loginToken(username, "clave456");
    }

    private long crearDireccion() throws Exception {
        String cuerpo = mockMvc.perform(post("/api/v1/clientes/direcciones")
                        .header("Authorization", "Bearer " + clienteToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"etiqueta":"Casa-%d","direccion":"Calle 123","telefono":"0999999999","observaciones":"azul"}
                                """.formatted(System.nanoTime())))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(cuerpo).path("id").asLong();
    }

    private void crearPedidoCliente(String codigo, String idem, long... productoIds) throws Exception {
        String items = Arrays.stream(productoIds)
                .mapToObj(p -> "{\"productoId\":%d,\"cantidad\":2,\"extraIds\":[],\"ingredientesRemovidos\":[]}"
                        .formatted(p))
                .collect(Collectors.joining(","));
        mockMvc.perform(post("/api/v1/clientes/pedidos")
                        .header("Authorization", "Bearer " + clienteToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "codigo":"%s",
                                  "metodoPago":"EFECTIVO",
                                  "metodoEntrega":"RETIRAR",
                                  "idempotencyKey":"%s",
                                  "items":[%s]
                                }
                                """.formatted(codigo, idem, items)))
                .andExpect(status().isOk());
    }

    private void confirmarCliente(String codigo, String idem) throws Exception {
        mockMvc.perform(post("/api/v1/clientes/pedidos/" + codigo + "/confirmar")
                        .header("Authorization", "Bearer " + clienteToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idempotencyKey":"%s"}
                                """.formatted(idem)))
                .andExpect(status().isOk());
    }


    private JsonNode buscar(String codigo, String estado) throws Exception {
        JsonNode todas = listar(estado);
        var result = new java.util.ArrayList<JsonNode>();
        for (JsonNode c : todas) {
            if (codigo.equals(c.path("pedidoCodigo").asText())) {
                result.add(c);
            }
        }
        return objectMapper.readTree(objectMapper.writeValueAsString(result));
    }

    private JsonNode buscarPendientes(String codigo) throws Exception {
        JsonNode todas = impresionPendientes();
        var result = new java.util.ArrayList<JsonNode>();
        for (JsonNode c : todas) {
            if (codigo.equals(c.path("pedidoCodigo").asText())) {
                result.add(c);
            }
        }
        return objectMapper.readTree(objectMapper.writeValueAsString(result));
    }

    private JsonNode listar(String estado) throws Exception {
        String url = "/api/v1/comandas";
        if (estado != null) {
            url += "?estado=" + estado;
        }
        MvcResult res = mockMvc.perform(get(url)
                        .header("Authorization", "Bearer " + cocineroToken))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(res.getResponse().getContentAsString());
    }

    private JsonNode impresionPendientes() throws Exception {
        MvcResult res = mockMvc.perform(get("/api/v1/comandas/impresion/pendientes")
                        .header("Authorization", "Bearer " + cocineroToken))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(res.getResponse().getContentAsString());
    }

    private JsonNode buscarPorArea(JsonNode comandas, long areaId) {
        for (JsonNode c : comandas) {
            if (c.path("areaId").asLong() == areaId) {
                return c;
            }
        }
        throw new AssertionError("No hay comanda para el área " + areaId);
    }

    private long crearArea(String nombre) throws Exception {
        String body = mockMvc.perform(post("/api/v1/areas")
                        .header("Authorization", "Bearer " + administradorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"%s-%d","descripcion":"área de prueba"}
                                """.formatted(nombre, System.nanoTime() % 100000)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).path("id").asLong();
    }

    private long crearProductoConArea(String nombre, String precio, long areaId) throws Exception {
        String body = mockMvc.perform(post("/api/v1/productos")
                        .header("Authorization", "Bearer " + administradorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"%s-%d","precio":%s,"areaId":%d,"extraIds":[],"ingredientes":[]}
                                """.formatted(nombre, System.nanoTime() % 100000, precio, areaId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).path("id").asLong();
    }

    private long crearProductoSinArea(String nombre, String precio) throws Exception {
        String body = mockMvc.perform(post("/api/v1/productos")
                        .header("Authorization", "Bearer " + administradorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"%s-%d","precio":%s,"extraIds":[],"ingredientes":[]}
                                """.formatted(nombre, System.nanoTime() % 100000, precio)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).path("id").asLong();
    }
}