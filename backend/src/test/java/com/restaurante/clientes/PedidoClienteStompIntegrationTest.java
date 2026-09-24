package com.restaurante.clientes;

import com.restaurante.AbstractIntegrationTest;
import com.restaurante.clientes.infrastructure.DifusorEstadoPedidoCliente;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A-05 (RF-43): E2E del canal en vivo del pedido del cliente con un servidor
 * real (RANDOM_PORT) y un cliente STOMP sobre WebSocket:
 * <ul>
 *   <li>el propietario recibe el estado inicial al suscribirse y el de cada
 *       transición vía outbox pedido-cliente-estado → tópico;</li>
 *   <li>otro cliente NO puede suscribirse al tópico ajeno (guardia A-01 y no
 *       recibe las difusiones).</li>
 * </ul>
 * El difusor no corre solo en tests (retardo de 1h en application-test.yml):
 * aquí se dispara a mano para que la aserción sea determinista.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.profiles.active=test")
class PedidoClienteStompIntegrationTest extends AbstractIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DifusorEstadoPedidoCliente difusor;

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5)).build();

    private String base;

    @BeforeEach
    void setUp() {
        base = "http://localhost:" + port;
    }

    @Test
    void propietarioRecibeEstadoInicialYLosCambiosPorOutbox() throws Exception {
        String admin = tokenDe("admin", "admin123");
        long productoId = crearProducto(admin, "ProdStomp-" + System.nanoTime(), "8.00");
        String codigo = "STOMP-" + System.nanoTime();
        String idem = "idem-stomp-" + System.nanoTime();

        crearYConfirmar(admin, clienteNuevo("cliente_e2e_a"), productoId, codigo, idem);

        WebSocketStompClient cliente = nuevoCliente();
        try {
            ClientStompSession clienteA = conectar(cliente,
                    tokenDe("cliente_e2e_a", passDe("cliente_e2e_a")));
            try {
                Captura captura = new Captura();
                StompSession sesion = clienteA.session();
                sesion.subscribe(PedidoClienteEventos.TOPIC_ESTADO + codigo, captura);

                // Estado inicial: snapshot REST al abrir la pantalla (el tópico
                // no tiene replay; los push cubren las transiciones posteriores).
                String inicial = httpGet("/api/v1/clientes/pedidos/" + codigo, tokenA(), 200);
                assertTrue(inicial.contains("\"estado\":\"CONFIRMADO\""),
                        "Snapshot inicial no CONFIRMADO: " + inicial);

                // Transición vía API + difusor -> push por STOMP
                httpSinCuerpo("/api/v1/clientes/pedidos/" + codigo + "/en-preparacion", admin);
                httpSinCuerpo("/api/v1/clientes/pedidos/" + codigo + "/listo", admin);
                difusor.difundirPendientes();

                String listo = captura.poll(
                        mensaje -> mensaje.contains("\"estado\":\"LISTO\""), 5);
                assertTrue(listo != null, "No llegó el estado LISTO por outbox: " + captura.frames);
            } finally {
                clienteA.close();
            }
        } finally {
            cliente.stop();
        }
    }

    private String tokenA() throws Exception {
        return tokenDe("cliente_e2e_a", passDe("cliente_e2e_a"));
    }

    @Test
    void otroClienteNoPuedeSuscribirseAlTopicAjeno() throws Exception {
        String admin = tokenDe("admin", "admin123");
        long productoId = crearProducto(admin, "ProdStompB-" + System.nanoTime(), "6.00");
        String codigo = "STOMPB-" + System.nanoTime();
        String idem = "idem-stompb-" + System.nanoTime();

        // El pedido lo compra el dueño...
        clienteNuevo("cliente_e2e_b");
        crearYConfirmar(admin, "cliente_e2e_b", productoId, codigo, idem);
        // ...y el intruso es OTRO cliente ajeno al pedido.
        clienteNuevo("cliente_e2e_c");
        String tokenIntruso = tokenDe("cliente_e2e_c", passDe("cliente_e2e_c"));

        WebSocketStompClient cliente = nuevoCliente();
        try {
            ClientStompSession intruso = conectar(cliente, tokenIntruso);
            Captura captura = new Captura();
            intruso.session().subscribe(PedidoClienteEventos.TOPIC_ESTADO + codigo, captura);

            // Difusión de un estado nuevo: solo el propietario debería recibirla.
            httpSinCuerpo("/api/v1/clientes/pedidos/" + codigo + "/en-preparacion", admin);
            httpSinCuerpo("/api/v1/clientes/pedidos/" + codigo + "/listo", admin);
            Thread.sleep(300);
            difusor.difundirPendientes();
            Thread.sleep(500);

            assertTrue(captura.frames.isEmpty(),
                    "El intruso recibió mensajes del tópico ajeno: " + captura.frames);
            intruso.close();
        } finally {
            cliente.stop();
        }
    }

    // ---- helpers HTTP (servidor real) ----

    private String tokenDe(String username, String password) throws Exception {
        var respuesta = http.send(HttpRequest.newBuilder(URI.create(base + "/api/v1/auth/login"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString("""
                                {"username":"%s","password":"%s"}
                                """.formatted(username, password)))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        return objectMapper.readTree(respuesta.body()).path("accessToken").asText();
    }

    private String http(String path, String cuerpo, String token, int esperado) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create(base + path))
                .header("Content-Type", "application/json");
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        if (cuerpo == null) {
            builder.POST(HttpRequest.BodyPublishers.noBody());
        } else {
            builder.POST(HttpRequest.BodyPublishers.ofString(cuerpo));
        }
        var respuesta = http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(esperado, respuesta.statusCode(),
                "POST " + path + " → " + respuesta.statusCode() + ": " + respuesta.body());
        return respuesta.body();
    }

    private String httpGet(String path, String token, int esperado) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create(base + path));
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        var respuesta = http.send(builder.GET().build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(esperado, respuesta.statusCode(),
                "GET " + path + " → " + respuesta.statusCode() + ": " + respuesta.body());
        return respuesta.body();
    }

    private void httpSinCuerpo(String path, String token) throws Exception {
        http(path, null, token, 200);
    }

    private long crearProducto(String admin, String nombre, String precio) throws Exception {
        String cuerpo = http("/api/v1/productos",
                """
                        {"nombre":"%s","precio":%s,"extraIds":[],"ingredientes":["cebolla"]}
                        """.formatted(nombre, precio), admin, 201);
        return objectMapper.readTree(cuerpo).path("id").asLong();
    }

    private String clienteNuevo(String username) throws Exception {
        String admin = tokenDe("admin", "admin123");
        http("/api/v1/usuarios",
                """
                        {"username":"%s","nombre":"%s","password":"clave_stomp_%s","rolCodigo":"CLIENTE"}
                        """.formatted(username, username, shortSufijo(username)), admin, 201);
        return username;
    }

    private void crearYConfirmar(String admin, String username, long productoId,
                                 String codigo, String idem) throws Exception {
        String token = tokenDe(username, passDe(username));
        String cuerpo = """
                {
                  "codigo":"%s",
                  "metodoPago":"EFECTIVO",
                  "metodoEntrega":"RETIRAR",
                  "idempotencyKey":"%s",
                  "items":[{"productoId":%d,"cantidad":1,"extraIds":[],"ingredientesRemovidos":[]}]
                }
                """.formatted(codigo, idem, productoId);
        http("/api/v1/clientes/pedidos", cuerpo, token, 200);
        http("/api/v1/clientes/pedidos/" + codigo + "/confirmar",
                """
                        {"idempotencyKey":"%s"}
                        """.formatted(idem), token, 200);
    }

    /** Password determinista por username para re-login entre tests del mismo contenedor. */
    private String passDe(String username) {
        return "clave_stomp_" + shortSufijo(username);
    }

    private String shortSufijo(String nombre) {
        return nombre.substring(nombre.length() - 4);
    }

    // ---- helpers STOMP ----

    /**
     * Cliente STOMP con el {@code SimpleMessageConverter} por defecto. Los frames
     * MESSAGE se suscriben pidiendo {@code byte[]} (el converter de Spring 7 ya no
     * traduce {@code byte[]}→{@code String} ni filtra por MIME: devuelve el payload
     * solo si es asignable al tipo pedido); la decodificación a JSON la hace el
     * {@code Captura}.
     */
    private WebSocketStompClient nuevoCliente() {
        return new WebSocketStompClient(new StandardWebSocketClient());
    }

    private ClientStompSession conectar(WebSocketStompClient cliente, String token)
            throws Exception {
        StompHeaders conecta = new StompHeaders();
        conecta.add("Authorization", "Bearer " + token);
        Captura captura = new Captura();
        StompSession sesion = cliente.connectAsync("ws://localhost:" + port + "/ws",
                (org.springframework.web.socket.WebSocketHttpHeaders) null, conecta, captura)
                .get(5, TimeUnit.SECONDS);
        assertTrue(sesion.isConnected(), "No se estableció la sesión STOMP: " + captura.frames);
        return new ClientStompSession(sesion);
    }

    /** Cierra la sesión y el cliente sin propagar errores del broker. */
    private record ClientStompSession(StompSession session) implements AutoCloseable {
        @Override
        public void close() {
            try {
                if (session != null && session.isConnected()) {
                    session.disconnect();
                }
            } catch (RuntimeException ignorado) {
                // el servidor pudo cerrar la sesión por rechazos (guardia A-01)
            }
        }
    }

    /** Captura los frames y ofrece una cola para esperar estados con timeout. */
    private static class Captura extends StompSessionHandlerAdapter {
        final ConcurrentLinkedQueue<String> frames = new ConcurrentLinkedQueue<>();

        @Override
        public java.lang.reflect.Type getPayloadType(StompHeaders headers) {
            return byte[].class;
        }

        @Override
        public void handleFrame(StompHeaders headers, Object payload) {
            if (payload instanceof byte[] bytes) {
                frames.add(new String(bytes, StandardCharsets.UTF_8));
            } else if (payload instanceof String texto) {
                frames.add(texto);
            } else {
                frames.add("payload:" + (payload == null ? "null" : payload.getClass().getSimpleName()));
            }
        }

        @Override
        public void handleException(StompSession session, StompCommand command,
                                    StompHeaders headers, byte[] payload, Throwable exception) {
            frames.add("excepcion:" + command + ":" + exception.getClass().getSimpleName());
        }

        @Override
        public void handleTransportError(StompSession session, Throwable exception) {
            frames.add("transport:" + exception.getClass().getSimpleName());
        }

        String poll(java.util.function.Predicate<String> filtro, int segundos) throws InterruptedException {
            long fin = System.currentTimeMillis() + segundos * 1000L;
            while (System.currentTimeMillis() < fin) {
                for (String frame : frames) {
                    if (filtro.test(frame)) {
                        return frame;
                    }
                }
                Thread.sleep(100);
            }
            return null;
        }
    }
}