package com.restaurante.catalogo.web;

import com.restaurante.catalogo.application.CategoriaService;
import com.restaurante.catalogo.web.dto.CategoriaRequest;
import com.restaurante.catalogo.web.dto.CategoriaResponse;
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
 * Administración de categorías del menú.
 */
@RestController
@RequestMapping("/api/v1/categorias")
@Tag(name = "catalogo", description = "Catálogo de productos")
public class CategoriaController {

    private final CategoriaService categoriaService;

    public CategoriaController(CategoriaService categoriaService) {
        this.categoriaService = categoriaService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('" + PermisoCodigo.CATALOGO_VER + "')")
    @Operation(summary = "Lista categorías")
    public List<CategoriaResponse> listar(@RequestParam(defaultValue = "false") boolean soloActivas) {
        return categoriaService.listar(soloActivas);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('" + PermisoCodigo.CATALOGO_VER + "')")
    @Operation(summary = "Obtiene una categoría por id")
    public CategoriaResponse obtener(@PathVariable Long id) {
        return categoriaService.obtener(id);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('" + PermisoCodigo.CATALOGO_GESTIONAR + "')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crea una categoría")
    public CategoriaResponse crear(@Valid @RequestBody CategoriaRequest request) {
        return categoriaService.crear(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('" + PermisoCodigo.CATALOGO_GESTIONAR + "')")
    @Operation(summary = "Actualiza una categoría")
    public CategoriaResponse actualizar(@PathVariable Long id,
                                        @Valid @RequestBody CategoriaRequest request) {
        return categoriaService.actualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('" + PermisoCodigo.CATALOGO_GESTIONAR + "')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Desactiva lógicamente una categoría")
    public void eliminar(@PathVariable Long id) {
        categoriaService.eliminar(id);
    }
}