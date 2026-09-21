package com.restaurante.comandas.web;

import com.restaurante.comandas.application.ComandaService;
import com.restaurante.comandas.domain.ComandaEstado;
import com.restaurante.comandas.web.dto.ComandaResponse;
import com.restaurante.comandas.web.dto.ErrorImpresionRequest;
import com.restaurante.comandas.web.dto.ImpresionResponse;
import com.restaurante.usuarios.PermisoCodigo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Comandas por área y órdenes de impresión (RF-11 a RF-14).
 */
@RestController
@RequestMapping("/api/v1/comandas")
@Tag(name = "comandas", description = "Comandas de cocina por área")
public class ComandaController {

    private final ComandaService comandaService;

    public ComandaController(ComandaService comandaService) {
        this.comandaService = comandaService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('" + PermisoCodigo.COMANDAS_VER + "')")
    @Operation(summary = "Lista comandas, opcionalmente por área y estado")
    public List<ComandaResponse> listar(@RequestParam(required = false) Long areaId,
                                        @RequestParam(required = false) ComandaEstado estado) {
        return comandaService.listar(areaId, estado);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('" + PermisoCodigo.COMANDAS_VER + "')")
    @Operation(summary = "Detalle de una comanda")
    public ComandaResponse obtener(@PathVariable Long id) {
        return comandaService.obtener(id);
    }

    @PostMapping("/{id}/en-preparacion")
    @PreAuthorize("hasAuthority('" + PermisoCodigo.PEDIDOS_ESTADO_PREPARACION + "')")
    @Operation(summary = "Marca la comanda en preparación y avanza el pedido")
    public ComandaResponse enPreparacion(@PathVariable Long id) {
        return comandaService.marcarEnPreparacion(id);
    }

    @PostMapping("/{id}/listo")
    @PreAuthorize("hasAuthority('" + PermisoCodigo.PEDIDOS_ESTADO_LISTO + "')")
    @Operation(summary = "Marca la comanda lista y avanza el pedido")
    public ComandaResponse listo(@PathVariable Long id) {
        return comandaService.marcarListo(id);
    }

    @PostMapping("/{id}/reimprimir")
    @PreAuthorize("hasAuthority('" + PermisoCodigo.COMANDAS_REENVIAR + "')")
    @Operation(summary = "Reenvía una orden de impresión para una comanda")
    public ComandaResponse reimprimir(@PathVariable Long id) {
        return comandaService.reimprimir(id);
    }

    @GetMapping("/impresion/pendientes")
    @PreAuthorize("hasAuthority('" + PermisoCodigo.COMANDAS_VER + "')")
    @Operation(summary = "Órdenes de impresión pendientes para el agente local")
    public List<ImpresionResponse> impresionPendientes() {
        return comandaService.imprimirPendientes();
    }

    @PostMapping("/impresion/{id}/enviado")
    @PreAuthorize("hasAuthority('" + PermisoCodigo.COMANDAS_VER + "')")
    @Operation(summary = "El agente confirma que imprimió la orden")
    public void enviadaImpresion(@PathVariable Long id) {
        comandaService.marcarEnviadaImpresion(id);
    }

    @PostMapping("/impresion/{id}/error")
    @PreAuthorize("hasAuthority('" + PermisoCodigo.COMANDAS_VER + "')")
    @Operation(summary = "El agente reporta que no pudo imprimir una orden")
    public void errorImpresion(@PathVariable Long id, @Valid @RequestBody ErrorImpresionRequest request) {
        comandaService.marcarErrorImpresion(id, request.motivo());
    }
}