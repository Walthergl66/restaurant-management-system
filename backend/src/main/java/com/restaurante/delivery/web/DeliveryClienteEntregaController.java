package com.restaurante.delivery.web;

import com.restaurante.delivery.application.RecorridoService;
import com.restaurante.delivery.application.SeguimientoEntregaSPI;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * App del CLIENTE: confirmación de recepción (2 de 2 de la doble confirmación) y
 * seguimiento de la entrega para la barra de progreso. Reutiliza los permisos de
 * cliente existentes y valida PROPIEDAD en backend (el cliente solo confirma o
 * consulta pedidos propios, resuelto vía la SPI de clientes). Vive en el módulo
 * delivery porque la doble confirmación reside en la parada (schema delivery),
 * pero el módulo delivery NO expone PII ni identidad interna del cliente.
 *
 * <p>Rutas bajo {@code /api/v1/clientes/pedidos/{codigo}/...} para que la app del
 * cliente las consuma junto al resto de endpoints de su pedido.
 */
@RestController
@RequestMapping("/api/v1/clientes/pedidos")
@Tag(name = "cliente-entrega",
        description = "Cliente: confirmar recepción (doble confirmación) y seguimiento de la entrega")
public class DeliveryClienteEntregaController {

    private final RecorridoService recorridos;

    public DeliveryClienteEntregaController(RecorridoService recorridos) {
        this.recorridos = recorridos;
    }

    @PostMapping("/{codigo}/confirmar-recepcion")
    @PreAuthorize("hasAuthority('clientes:pedido-confirmar')")
    @Operation(summary = "El cliente confirma la recepción de su pedido (idempotente por Idempotency-Key)")
    public SeguimientoEntregaSPI confirmarRecepcion(
            @PathVariable String codigo,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        return recorridos.confirmarRecepcionCliente(codigo, usernameActual(), idempotencyKey);
    }

    @GetMapping("/{codigo}/seguimiento")
    @PreAuthorize("hasAuthority('clientes:historial-ver')")
    @Operation(summary = "Seguimiento de la entrega (fase logística + confirmación) para la barra de progreso")
    public SeguimientoEntregaSPI seguimiento(@PathVariable String codigo) {
        return recorridos.seguimientoDePedido(codigo, usernameActual());
    }

    private String usernameActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new IllegalStateException("No autenticado");
        }
        return auth.getName();
    }
}
