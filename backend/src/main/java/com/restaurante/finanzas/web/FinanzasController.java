package com.restaurante.finanzas.web;

import com.restaurante.finanzas.application.FinanzasService;
import com.restaurante.usuarios.PermisoCodigo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

/**
 * Finanzas (RF-36 a RF-39): ingresos por cobros, egresos manuales y resultado
 * entre dos instantes.
 */
@RestController
@RequestMapping("/api/v1/finanzas")
@Tag(name = "finanzas", description = "Resumen financiero por período")
public class FinanzasController {

    private final FinanzasService finanzasService;

    public FinanzasController(FinanzasService finanzasService) {
        this.finanzasService = finanzasService;
    }

    @GetMapping("/resumen")
    @PreAuthorize("hasAuthority('" + PermisoCodigo.FINANZAS_VER + "')")
    @Operation(summary = "Ingresos, egresos y resultado del período")
    public FinanzasService.Resumen resumen(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant hasta) {
        return finanzasService.resumen(desde, hasta);
    }
}