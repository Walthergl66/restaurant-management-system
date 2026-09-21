package com.restaurante.catalogo.web;

import com.restaurante.catalogo.application.ExtraService;
import com.restaurante.catalogo.web.dto.ExtraRequest;
import com.restaurante.catalogo.web.dto.ExtraResponse;
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
 * Administración de extras (adiciones/toppings).
 */
@RestController
@RequestMapping("/api/v1/extras")
@Tag(name = "catalogo", description = "Catálogo de productos")
public class ExtraController {

    private final ExtraService extraService;

    public ExtraController(ExtraService extraService) {
        this.extraService = extraService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('" + PermisoCodigo.CATALOGO_VER + "')")
    @Operation(summary = "Lista extras")
    public List<ExtraResponse> listar(@RequestParam(defaultValue = "false") boolean soloActivos) {
        return extraService.listar(soloActivos);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('" + PermisoCodigo.CATALOGO_VER + "')")
    @Operation(summary = "Obtiene un extra por id")
    public ExtraResponse obtener(@PathVariable Long id) {
        return extraService.obtener(id);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('" + PermisoCodigo.CATALOGO_GESTIONAR + "')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crea un extra")
    public ExtraResponse crear(@Valid @RequestBody ExtraRequest request) {
        return extraService.crear(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('" + PermisoCodigo.CATALOGO_GESTIONAR + "')")
    @Operation(summary = "Actualiza un extra")
    public ExtraResponse actualizar(@PathVariable Long id,
                                    @Valid @RequestBody ExtraRequest request) {
        return extraService.actualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('" + PermisoCodigo.CATALOGO_GESTIONAR + "')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Desactiva lógicamente un extra")
    public void eliminar(@PathVariable Long id) {
        extraService.eliminar(id);
    }
}