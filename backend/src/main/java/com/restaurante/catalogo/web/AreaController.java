package com.restaurante.catalogo.web;

import com.restaurante.catalogo.application.AreaService;
import com.restaurante.catalogo.web.dto.AreaRequest;
import com.restaurante.catalogo.web.dto.AreaResponse;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Administración de áreas de preparación.
 */
@RestController
@RequestMapping("/api/v1/areas")
@Tag(name = "catalogo", description = "Catálogo de productos")
public class AreaController {

    private final AreaService areaService;

    public AreaController(AreaService areaService) {
        this.areaService = areaService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('" + PermisoCodigo.CATALOGO_VER + "')")
    @Operation(summary = "Lista áreas de preparación")
    public List<AreaResponse> listar(@RequestParam(defaultValue = "false") boolean soloActivas) {
        return areaService.listar(soloActivas);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('" + PermisoCodigo.CATALOGO_VER + "')")
    @Operation(summary = "Obtiene un área por id")
    public AreaResponse obtener(@PathVariable Long id) {
        return areaService.obtener(id);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('" + PermisoCodigo.CATALOGO_GESTIONAR + "')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crea un área")
    public AreaResponse crear(@Valid @RequestBody AreaRequest request) {
        return areaService.crear(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('" + PermisoCodigo.CATALOGO_GESTIONAR + "')")
    @Operation(summary = "Actualiza un área")
    public AreaResponse actualizar(@PathVariable Long id,
                                   @Valid @RequestBody AreaRequest request) {
        return areaService.actualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('" + PermisoCodigo.CATALOGO_GESTIONAR + "')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Desactiva lógicamente un área")
    public void eliminar(@PathVariable Long id) {
        areaService.eliminar(id);
    }
}