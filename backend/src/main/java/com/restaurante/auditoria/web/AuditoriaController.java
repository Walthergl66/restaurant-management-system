package com.restaurante.auditoria.web;

import com.restaurante.auditoria.AuditoriaResumen;
import com.restaurante.auditoria.application.AuditoriaService;
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
import java.util.List;

/**
 * Consulta del historial de operaciones (RF-52). Solo lectura: la escritura la
 * hace el propio sistema desde los eventos de negocio.
 */
@RestController
@RequestMapping("/api/v1/auditoria")
@Tag(name = "auditoria", description = "Historial de operaciones (solo lectura)")
public class AuditoriaController {

    private final AuditoriaService auditoriaService;

    public AuditoriaController(AuditoriaService auditoriaService) {
        this.auditoriaService = auditoriaService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('" + PermisoCodigo.AUDITORIA_VER + "')")
    @Operation(summary = "Eventos de auditoría por período (y opcionalmente por entidad)")
    public List<AuditoriaResumen> listar(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant hasta,
            @RequestParam(required = false) String entidad) {
        return auditoriaService.listar(desde, hasta, entidad);
    }
}