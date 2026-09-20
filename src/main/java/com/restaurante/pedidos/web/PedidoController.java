package com.restaurante.pedidos.web;

import com.restaurante.pedidos.application.PedidoService;
import com.restaurante.pedidos.web.dto.ActualizarLineaRequest;
import com.restaurante.pedidos.web.dto.AgregarLineaRequest;
import com.restaurante.pedidos.web.dto.CrearPedidoRequest;
import com.restaurante.pedidos.web.dto.PedidoResponse;
import com.restaurante.usuarios.PermisoCodigo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Pedidos presenciales: borrador, personalización, resumen y confirmación.
 */
@RestController
@RequestMapping("/api/v1/pedidos")
@Tag(name = "pedidos", description = "Pedidos presenciales")
public class PedidoController {

    private final PedidoService pedidoService;

    public PedidoController(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('" + PermisoCodigo.PEDIDOS_CREAR + "')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crea un pedido en borrador y ocupa la mesa")
    public PedidoResponse crear(@Valid @RequestBody CrearPedidoRequest request) {
        return pedidoService.crear(request);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('" + PermisoCodigo.PEDIDOS_VER + "')")
    @Operation(summary = "Lista pedidos (por mesa opcionalmente)")
    public List<PedidoResponse> listar(@RequestParam(required = false) Long mesaId) {
        return pedidoService.listar(mesaId);
    }

    @GetMapping("/{codigo}")
    @PreAuthorize("hasAuthority('" + PermisoCodigo.PEDIDOS_VER + "')")
    @Operation(summary = "Resumen completo de un pedido")
    public PedidoResponse obtener(@PathVariable String codigo) {
        return pedidoService.obtener(codigo);
    }

    @PostMapping("/{codigo}/items")
    @PreAuthorize("hasAuthority('" + PermisoCodigo.PEDIDOS_EDITAR + "')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Agrega una línea al pedido (solo en borrador)")
    public PedidoResponse agregarLinea(@PathVariable String codigo,
                                       @Valid @RequestBody AgregarLineaRequest request) {
        return pedidoService.agregarLinea(codigo, request);
    }

    @PutMapping("/{codigo}/items/{lineaId}")
    @PreAuthorize("hasAuthority('" + PermisoCodigo.PEDIDOS_EDITAR + "')")
    @Operation(summary = "Reemplaza cantidad, extras y anotaciones de una línea")
    public PedidoResponse actualizarLinea(@PathVariable String codigo,
                                          @PathVariable Long lineaId,
                                          @Valid @RequestBody ActualizarLineaRequest request) {
        return pedidoService.actualizarLinea(codigo, lineaId, request);
    }

    @DeleteMapping("/{codigo}/items/{lineaId}")
    @PreAuthorize("hasAuthority('" + PermisoCodigo.PEDIDOS_EDITAR + "')")
    @Operation(summary = "Quita una línea del pedido (solo en borrador)")
    public PedidoResponse quitarLinea(@PathVariable String codigo,
                                      @PathVariable Long lineaId) {
        return pedidoService.quitarLinea(codigo, lineaId);
    }

    @PostMapping("/{codigo}/confirmar")
    @PreAuthorize("hasAuthority('" + PermisoCodigo.PEDIDOS_CONFIRMAR + "')")
    @Operation(summary = "Confirma el pedido (congela precios y bloquea cambios)")
    public PedidoResponse confirmar(@PathVariable String codigo,
                                    @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey) {
        return pedidoService.confirmar(codigo, idempotencyKey);
    }

    @DeleteMapping("/{codigo}")
    @PreAuthorize("hasAuthority('" + PermisoCodigo.PEDIDOS_EDITAR + "')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Cancela un borrador y libera la mesa")
    public void cancelar(@PathVariable String codigo) {
        pedidoService.cancelarBorrador(codigo);
    }
}