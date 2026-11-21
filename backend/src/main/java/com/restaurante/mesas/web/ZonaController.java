package com.restaurante.mesas.web;

import com.restaurante.mesas.application.ZonaService;
import com.restaurante.mesas.web.dto.OrdenZonasRequest;
import com.restaurante.mesas.web.dto.ZonaRequest;
import com.restaurante.mesas.web.dto.ZonaResponse;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Zonas del restaurante: las define el administrador y agrupan las mesas. */
@RestController
@RequestMapping("/api/v1/mesas/zonas")
@Tag(name = "mesas", description = "Mesas y zonas del salón")
public class ZonaController {

    private final ZonaService zonaService;

    public ZonaController(ZonaService zonaService) {
        this.zonaService = zonaService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('" + PermisoCodigo.MESAS_VER + "')")
    @Operation(summary = "Lista las zonas en su orden, con el número de mesas de cada una")
    public List<ZonaResponse> listar() {
        return zonaService.listar().stream().map(ZonaResponse::from).toList();
    }

    @PostMapping
    @PreAuthorize("hasAuthority('" + PermisoCodigo.MESAS_GESTIONAR + "')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crea una zona (queda al final del orden)")
    public ZonaResponse crear(@Valid @RequestBody ZonaRequest request) {
        return ZonaResponse.from(zonaService.crear(request.nombre()));
    }

    @PutMapping("/orden")
    @PreAuthorize("hasAuthority('" + PermisoCodigo.MESAS_GESTIONAR + "')")
    @Operation(summary = "Guarda el orden de todas las zonas")
    public List<ZonaResponse> reordenar(@Valid @RequestBody OrdenZonasRequest request) {
        return zonaService.reordenar(request.ids()).stream().map(ZonaResponse::from).toList();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('" + PermisoCodigo.MESAS_GESTIONAR + "')")
    @Operation(summary = "Renombra una zona")
    public ZonaResponse renombrar(@PathVariable Long id, @Valid @RequestBody ZonaRequest request) {
        return ZonaResponse.from(zonaService.renombrar(id, request.nombre()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('" + PermisoCodigo.MESAS_GESTIONAR + "')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Elimina una zona vacía (siempre debe quedar al menos una)")
    public void eliminar(@PathVariable Long id) {
        zonaService.eliminar(id);
    }
}
