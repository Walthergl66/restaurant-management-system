package com.restaurante.reportes.web;

import com.restaurante.reportes.application.ReportesService;
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
 * Reportes de ventas (RF-51, RF-52).
 */
@RestController
@RequestMapping("/api/v1/reportes")
@Tag(name = "reportes", description = "Reportes de ventas")
public class ReportesController {

    private final ReportesService reportesService;

    public ReportesController(ReportesService reportesService) {
        this.reportesService = reportesService;
    }

    @GetMapping("/ventas")
    @PreAuthorize("hasAuthority('" + PermisoCodigo.REPORTES_VER + "')")
    @Operation(summary = "Ventas del período: total, por método, por día y por producto")
    public ReportesService.ReporteVentas ventas(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant hasta) {
        return reportesService.ventas(desde, hasta);
    }
}