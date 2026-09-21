package com.restaurante.anulaciones.web;

import com.restaurante.anulaciones.application.AnulacionService;
import com.restaurante.anulaciones.domain.EstadoAnulacion;
import com.restaurante.anulaciones.web.dto.AnulacionResponse;
import com.restaurante.anulaciones.web.dto.SolicitarAnulacionRequest;
import com.restaurante.usuarios.PermisoCodigo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Anulaciones: solicitud sobre una línea confirmada, aprobación (genera la
 * comanda de cancelación y descuenta de la cuenta) y rechazo (RF-20 a RF-23).
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "anulaciones", description = "Anulaciones de líneas de pedido")
public class AnulacionController {

    private final AnulacionService anulacionService;

    public AnulacionController(AnulacionService anulacionService) {
        this.anulacionService = anulacionService;
    }

    @PostMapping("/pedidos/{codigo}/items/{lineaId}/anulaciones")
    @PreAuthorize("hasAuthority('" + PermisoCodigo.ANULACIONES_SOLICITAR + "')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Solicita anular una línea de un pedido confirmado")
    public AnulacionResponse solicitar(@PathVariable String codigo,
                                       @PathVariable Long lineaId,
                                       @Valid @RequestBody SolicitarAnulacionRequest request) {
        return anulacionService.solicitar(codigo, lineaId, request);
    }

    @PatchMapping("/anulaciones/{id}/aprobar")
    @PreAuthorize("hasAuthority('" + PermisoCodigo.ANULACIONES_APROBAR + "')")
    @Operation(summary = "Aprueba la anulación (descuenta de la cuenta y genera comanda de cancelación)")
    public AnulacionResponse aprobar(@PathVariable Long id) {
        return anulacionService.aprobar(id);
    }

    @PatchMapping("/anulaciones/{id}/rechazar")
    @PreAuthorize("hasAuthority('" + PermisoCodigo.ANULACIONES_APROBAR + "')")
    @Operation(summary = "Rechaza la anulación (no descuenta)")
    public AnulacionResponse rechazar(@PathVariable Long id) {
        return anulacionService.rechazar(id);
    }

    @GetMapping("/anulaciones")
    @PreAuthorize("hasAnyAuthority('" + PermisoCodigo.ANULACIONES_SOLICITAR + "', '" + PermisoCodigo.ANULACIONES_APROBAR + "')")
    @Operation(summary = "Lista anulaciones (por pedido o estado)")
    public List<AnulacionResponse> listar(@RequestParam(required = false) String pedidoCodigo,
                                          @RequestParam(required = false) EstadoAnulacion estado) {
        return anulacionService.listar(pedidoCodigo, estado);
    }
}