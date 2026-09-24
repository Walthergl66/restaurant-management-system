package com.restaurante.clientes.web;

import com.restaurante.clientes.CarritoClienteSPI;
import com.restaurante.clientes.Clientes;
import com.restaurante.clientes.DireccionClienteSPI;
import com.restaurante.clientes.PedidoClienteSPI;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** RF-40 a RF-45: controlador de la app del cliente. RF-41 carrito+confirmar
 *  idempotente (RF-24/25: misma clave 200, clave distinta 409), RF-43 push
 *  en tiempo real v&iacute;a WebSocket (outbox V4), RF-44 tablet del mesero
 *  marca EN_PREPARACION/LISTO (RF-22/23/24/25 RF-15/16 escal&oacute;n RF-06/07). */
@RestController
@RequestMapping("/api/v1/clientes")
@Tag(name = "clientes", description = "Pedidos de la app del cliente (RF-40..45)")
public class ClientesController {

    private final Clientes clientes;

    public ClientesController(Clientes clientes) {
        this.clientes = clientes;
    }

    /** RF-40: carrito del cliente logueado. RF-41 SPI Money men&uacute;. */
    @GetMapping("/carrito")
    @PreAuthorize("hasAuthority('clientes:carrito-gestionar')")
    @Operation(summary = "Carrito del cliente (BORRADOR)")
    public CarritoClienteSPI carrito() {
        return clientes.carrito(clienteIdActual());
    }

    /** RF-40: ver men&uacute; desde la app (RF-41 SPI Catalogo/Money). */
    @GetMapping("/menu")
    @PreAuthorize("hasAuthority('clientes:menu-ver')")
    @Operation(summary = "Menú para la app del cliente")
    public List<com.restaurante.catalogo.ProductoParaPedido> menu() {
        return clientes.menu();
    }

    /** RF-41: crear pedido (carrito -> BORRADOR) idempotente. */
    @PostMapping("/pedidos")
    @PreAuthorize("hasAuthority('clientes:pedido-crear')")
    @Operation(summary = "Crear pedido cliente (BORRADOR) idempotente")
    public PedidoClienteSPI crearPedido(@Valid @RequestBody CrearPedidoClienteRequest req) {
        return clientes.crearPedido(clienteIdActual(), req);
    }

    /** RF-41: confirmar (congela total Money RF-41). Mismo pedido+clave =
     *  idempotente 200; clave de otro pedido = 409 (RF-24/25 RF-41 RNF-17). */
    @PostMapping("/pedidos/{codigo}/confirmar")
    @PreAuthorize("hasAuthority('clientes:pedido-confirmar')")
    @Operation(summary = "Confirmar pedido cliente idempotente")
    public PedidoClienteSPI confirmar(@PathVariable String codigo,
                                      @Valid @RequestBody ConfirmarPedidoClienteRequest req) {
        return clientes.confirmar(clienteIdActual(), codigo, req);
    }

    /** RF-44: tablet del mesero RF-15/24 tablet RF-22 marca EN_PREPARACION. */
    @PostMapping("/pedidos/{codigo}/en-preparacion")
    @PreAuthorize("hasAnyAuthority('pedidos:estado-preparacion', 'clientes:gestionar')")
    @Operation(summary = "Tablet marca EN_PREPARACION")
    public PedidoClienteSPI enPreparacion(@PathVariable String codigo) {
        return clientes.marcarEnPreparacion(codigo);
    }

    /** RF-24/tablet RF-25: tablet del mesero marca LISTO (RF-44). */
    @PostMapping("/pedidos/{codigo}/listo")
    @PreAuthorize("hasAnyAuthority('pedidos:estado-listo', 'clientes:gestionar')")
    @Operation(summary = "Tablet marca LISTO")
    public PedidoClienteSPI listo(@PathVariable String codigo) {
        return clientes.marcarListo(codigo);
    }

    /** RF-43: el cliente se suscribe al tópico y recibe en vivo cada cambio de
     *  estado (broker + outbox). El estado inicial NO viaja por el tópico (sin
     *  replay): se obtiene con este snapshot v&iacute;a REST al abrir la pantalla. */
    @GetMapping("/pedidos/{codigo}")
    @PreAuthorize("hasAnyAuthority('clientes:historial-ver', 'clientes:gestionar')")
    @Operation(summary = "Estado/snapshot de un pedido propio")
    public PedidoClienteSPI pedido(@PathVariable String codigo) {
        return clientes.pedidoSPIporCodigo(clienteIdActual(), codigo);
    }

    /** RF-45: historial del cliente paginado (A-10: page size máximo 100,
     *  orden descendente por id = últimos primero). */
    @GetMapping("/historial")
    @PreAuthorize("hasAuthority('clientes:historial-ver')")
    @Operation(summary = "Historial paginado del cliente")
    public org.springframework.data.domain.Page<PedidoClienteSPI> historial(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        int limite = Math.min(Math.max(size, 1), 100);
        return clientes.historial(clienteIdActual(),
                org.springframework.data.domain.PageRequest.of(page, limite,
                        org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "id")));
    }

    /** Tabla RF-14 agrega direcci&oacute;n RF-42 (Domicilio RF-42/43). */
    @PostMapping("/direcciones")
    @PreAuthorize("hasAnyAuthority('clientes:carrito-gestionar', 'clientes:cuenta-nueva', 'clientes:gestionar')")
    @Operation(summary = "Nueva dirección para domicilio")
    public DireccionClienteSPI nuevaDireccion(@Valid @RequestBody NuevaDireccionClienteRequest req) {
        return clientes.nuevaDireccion(clienteIdActual(), req);
    }

    private Long clienteIdActual() {
        org.springframework.security.core.Authentication auth =
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new IllegalStateException("No autenticado (RF-40)");
        }
        return clientes.resolverClienteId(auth.getName());
    }
}
