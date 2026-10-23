package com.restaurante.clientes;

import com.restaurante.AbstractIntegracionApi;
import com.restaurante.clientes.domain.PedidoCliente;
import com.restaurante.clientes.infrastructure.PedidoClienteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Recargo fijo de $1.50 para pedidos DOMICILIO: el backend lo calcula,
 * persiste y congela al confirmar, y no lo duplica en reintentos idempotentes.
 */
class ClientesRecargoDomicilioIntegrationTest extends AbstractIntegracionApi {

    @Autowired
    private PedidoClienteRepository pedidoRepository;

    private String adminToken;
    private String clienteToken;

    @BeforeEach
    void preparar() {
        adminToken = tokenAdmin();
        clienteToken = tokenCliente("cliente_recargo", "claveRec123");
    }

    @Test
    void domicilioConfirmadoSumaRecargoYLoCongelaEnBD() throws Exception {
        long productoId = crearProducto("ProdRecDom-" + System.nanoTime(), "10.00");
        long direccionId = crearDireccion();
        String codigo = "REC-DOM-" + System.nanoTime();
        String idem = "idem-rec-dom-" + System.nanoTime();

        // En BORRADOR el checkout ya proyecta el desglose (subtotal + recargo).
        mockMvc.perform(post("/api/v1/clientes/pedidos")
                        .header("Authorization", "Bearer " + clienteToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoDomicilio(codigo, idem, direccionId, productoId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("BORRADOR"))
                .andExpect(jsonPath("$.subtotal").value(10.00))
                .andExpect(jsonPath("$.costoEntrega").value(1.50))
                .andExpect(jsonPath("$.total").value(11.50));

        // Al confirmar se congela el recargo.
        mockMvc.perform(post("/api/v1/clientes/pedidos/" + codigo + "/confirmar")
                        .header("Authorization", "Bearer " + clienteToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idempotencyKey":"%s"}
                                """.formatted(idem)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CONFIRMADO"))
                .andExpect(jsonPath("$.subtotal").value(10.00))
                .andExpect(jsonPath("$.costoEntrega").value(1.50))
                .andExpect(jsonPath("$.total").value(11.50));

        // Persistido y congelado en la columna costo_entrega.
        PedidoCliente guardado = pedidoRepository.findByCodigo(codigo).orElseThrow();
        assertEquals(0, new BigDecimal("1.50").compareTo(guardado.getCostoEntrega().getAmount()));
    }

    @Test
    void retirarNoTieneRecargo() throws Exception {
        long productoId = crearProducto("ProdRecRet-" + System.nanoTime(), "8.00");
        String codigo = "REC-RET-" + System.nanoTime();
        String idem = "idem-rec-ret-" + System.nanoTime();

        mockMvc.perform(post("/api/v1/clientes/pedidos")
                        .header("Authorization", "Bearer " + clienteToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoRetirar(codigo, idem, productoId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subtotal").value(8.00))
                .andExpect(jsonPath("$.costoEntrega").value(0.00))
                .andExpect(jsonPath("$.total").value(8.00));

        mockMvc.perform(post("/api/v1/clientes/pedidos/" + codigo + "/confirmar")
                        .header("Authorization", "Bearer " + clienteToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idempotencyKey":"%s"}
                                """.formatted(idem)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.costoEntrega").value(0.00))
                .andExpect(jsonPath("$.total").value(8.00));

        PedidoCliente guardado = pedidoRepository.findByCodigo(codigo).orElseThrow();
        assertEquals(0, new BigDecimal("0.00").compareTo(guardado.getCostoEntrega().getAmount()));
    }

    @Test
    void reconfirmarIdempotenteNoDuplicaElRecargo() throws Exception {
        long productoId = crearProducto("ProdRecIdem-" + System.nanoTime(), "10.00");
        long direccionId = crearDireccion();
        String codigo = "REC-IDEM-" + System.nanoTime();
        String idem = "idem-rec-idem-" + System.nanoTime();

        mockMvc.perform(post("/api/v1/clientes/pedidos")
                        .header("Authorization", "Bearer " + clienteToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoDomicilio(codigo, idem, direccionId, productoId)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/clientes/pedidos/" + codigo + "/confirmar")
                        .header("Authorization", "Bearer " + clienteToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idempotencyKey":"%s"}
                                """.formatted(idem)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(11.50));

        // Reintento idempotente (misma clave): el total sigue 11.50, NO 13.00.
        mockMvc.perform(post("/api/v1/clientes/pedidos/" + codigo + "/confirmar")
                        .header("Authorization", "Bearer " + clienteToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idempotencyKey":"%s"}
                                """.formatted(idem)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.costoEntrega").value(1.50))
                .andExpect(jsonPath("$.total").value(11.50));

        PedidoCliente guardado = pedidoRepository.findByCodigo(codigo).orElseThrow();
        assertEquals(0, new BigDecimal("1.50").compareTo(guardado.getCostoEntrega().getAmount()));
    }

    // ---- helpers ----

    private long crearDireccion() throws Exception {
        MvcResult res = mockMvc.perform(post("/api/v1/clientes/direcciones")
                        .header("Authorization", "Bearer " + clienteToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"etiqueta":"Casa-%d","direccion":"Calle 123","telefono":"0999999999","observaciones":"azul"}
                                """.formatted(System.nanoTime())))
                .andExpect(status().isOk())
                .andReturn();
        long id = objectMapper.readTree(res.getResponse().getContentAsString()).path("id").asLong();
        assertTrue(id > 0);
        return id;
    }

    private String cuerpoDomicilio(String codigo, String idem, long direccionId, long productoId) {
        return """
                {
                  "codigo":"%s",
                  "metodoPago":"EFECTIVO",
                  "metodoEntrega":"DOMICILIO",
                  "direccionId":%d,
                  "idempotencyKey":"%s",
                  "items":[{"productoId":%d,"cantidad":1,"extraIds":[],"ingredientesRemovidos":[]}]
                }
                """.formatted(codigo, direccionId, idem, productoId);
    }

    private String cuerpoRetirar(String codigo, String idem, long productoId) {
        return """
                {
                  "codigo":"%s",
                  "metodoPago":"EFECTIVO",
                  "metodoEntrega":"RETIRAR",
                  "idempotencyKey":"%s",
                  "items":[{"productoId":%d,"cantidad":1,"extraIds":[],"ingredientesRemovidos":[]}]
                }
                """.formatted(codigo, idem, productoId);
    }

    private long crearProducto(String nombre, String precio) throws Exception {
        String body = mockMvc.perform(post("/api/v1/productos")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"%s","precio":%s,"extraIds":[],"ingredientes":["cebolla"]}
                                """.formatted(nombre, precio)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).path("id").asLong();
    }
}
