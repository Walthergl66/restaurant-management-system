package com.restaurante.facturacion.web;

import com.restaurante.facturacion.ComprobanteResumen;
import com.restaurante.facturacion.application.ComprobanteService;
import com.restaurante.usuarios.PermisoCodigo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

/**
 * Comprobantes emitidos (RF-28 a RF-30). La visión del comprobante es
 * solo-lectura para cajeros y administradores.
 */
@RestController
@RequestMapping("/api/v1/comprobantes")
@Tag(name = "facturacion", description = "Comprobantes emitidos")
public class ComprobanteController {

    private final ComprobanteService comprobanteService;

    public ComprobanteController(ComprobanteService comprobanteService) {
        this.comprobanteService = comprobanteService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('" + PermisoCodigo.FACTURACION_EMITIR + "')")
    @Operation(summary = "Lista comprobantes (por cuenta o rango de fechas)")
    public List<ComprobanteResumen> listar(
            @RequestParam(required = false) Long cuentaId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant hasta) {
        return comprobanteService.listar(cuentaId, desde, hasta);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('" + PermisoCodigo.FACTURACION_EMITIR + "')")
    @Operation(summary = "Detalle de un comprobante")
    public ComprobanteResumen obtener(@PathVariable Long id) {
        return comprobanteService.obtener(id);
    }
}