package com.restaurante.clientes.web;

import com.restaurante.clientes.CarritoClienteSPI;
import com.restaurante.clientes.Clientes;
import com.restaurante.clientes.DireccionClienteSPI;
import com.restaurante.clientes.PedidoClienteSPI;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** RF-40 a RF-45: controlador de la app del cliente. RF-41 carrito+confirmar
 *  idempotente (RF-24/25: misma clave 200, clave distinta 409), RF-43 push
 *  en tiempo real v&iacute;a WebSocket (outbox V4), RF-44 tablet del mesero
 *  marca EN_PREPARACION/LISTO (RF-22/23/24/25 RF-15/16 escal&oacute;n RF-06/07). */
@RestController
@RequestMapping("/api/v1/clientes")
public class ClientesController {

    private final Clientes clientes;

    public ClientesController(Clientes clientes) {
        this.clientes = clientes;
    }

    /** RF-40: carrito del cliente logueado. RF-41 SPI Money men&uacute;. */
    @GetMapping("/carrito")
    @PreAuthorize("hasAuthority('clientes:carrito-gestionar')")
    public CarritoClienteSPI carrito() {
        return clientes.carrito(clienteIdActual());
    }

    /** RF-40: ver men&uacute; desde la app (RF-41 SPI Catalogo/Money). */
    @GetMapping("/menu")
    @PreAuthorize("hasAuthority('clientes:menu-ver')")
    public List<com.restaurante.catalogo.ProductoParaPedido> menu() {
        return clientes.menu();
    }

    /** RF-41: crear pedido (carrito -> BORRADOR) idempotente. */
    @PostMapping("/pedidos")
    @PreAuthorize("hasAuthority('clientes:pedido-crear')")
    public PedidoClienteSPI crearPedido(@RequestBody CrearPedidoClienteRequest req) {
        return clientes.crearPedido(clienteIdActual(), req);
    }

    /** RF-41: confirmar (congela total Money RF-41). Mismo pedido+clave =
     *  idempotente 200; clave de otro pedido = 409 (RF-24/25 RF-41 RNF-17). */
    @PostMapping("/pedidos/{codigo}/confirmar")
    @PreAuthorize("hasAuthority('clientes:pedido-confirmar')")
    public PedidoClienteSPI confirmar(@PathVariable String codigo,
                                      @RequestBody ConfirmarPedidoClienteRequest req) {
        return clientes.confirmar(codigo, req);
    }

    /** RF-44: tablet del mesero RF-15/24 tablet RF-22 marca EN_PREPARACION. */
    @PostMapping("/pedidos/{codigo}/en-preparacion")
    @PreAuthorize("hasAnyAuthority('pedidos:estado-preparacion', 'clientes:gestionar')")
    public PedidoClienteSPI enPreparacion(@PathVariable String codigo) {
        return clientes.marcarEnPreparacion(codigo);
    }

    /** RF-24/tablet RF-25: tablet del mesero marca LISTO (RF-44). */
    @PostMapping("/pedidos/{codigo}/listo")
    @PreAuthorize("hasAnyAuthority('pedidos:estado-listo', 'clientes:gestionar')")
    public PedidoClienteSPI listo(@PathVariable String codigo) {
        return clientes.marcarListo(codigo);
    }

    /** RF-43: el cliente se suscribe y recibe el estado en tiempo real. */
    @SubscribeMapping("/topic/pedido/{codigo}")
    public PedidoClienteSPI estadoEnVivo(@DestinationVariable String codigo) {
        return clientes.pedidoSPIporCodigo(codigo);
    }

    /** RF-45: historial del cliente. RF-43 RF-44: solo lectura. */
    @GetMapping("/historial")
    @PreAuthorize("hasAuthority('clientes:historial-ver')")
    public List<PedidoClienteSPI> historial() {
        return clientes.historial(clienteIdActual());
    }

    /** Tabla RF-14 agrega direcci&oacute;n RF-42 (Domicilio RF-42/43). */
    @PostMapping("/direcciones")
    @PreAuthorize("hasAnyAuthority('clientes:carrito-gestionar', 'clientes:cuenta-nueva', 'clientes:gestionar')")
    public DireccionClienteSPI nuevaDireccion(@RequestBody NuevaDireccionClienteRequest req) {
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
