package com.restaurante.catalogo.web;

import com.restaurante.catalogo.application.ProductoService;
import com.restaurante.catalogo.web.dto.ProductoRequest;
import com.restaurante.catalogo.web.dto.ProductoResponse;
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

/**
 * Administración de productos del catálogo.
 */
@RestController
@RequestMapping("/api/v1/productos")
@Tag(name = "catalogo", description = "Catálogo de productos")
public class ProductoController {

    private final ProductoService productoService;

    public ProductoController(ProductoService productoService) {
        this.productoService = productoService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('" + PermisoCodigo.CATALOGO_VER + "')")
    @Operation(summary = "Lista productos")
    public List<ProductoResponse> listar() {
        return productoService.listar();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('" + PermisoCodigo.CATALOGO_VER + "')")
    @Operation(summary = "Obtiene un producto por id")
    public ProductoResponse obtener(@PathVariable Long id) {
        return productoService.obtener(id);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('" + PermisoCodigo.CATALOGO_GESTIONAR + "')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crea un producto")
    public ProductoResponse crear(@Valid @RequestBody ProductoRequest request) {
        return productoService.crear(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('" + PermisoCodigo.CATALOGO_GESTIONAR + "')")
    @Operation(summary = "Actualiza un producto")
    public ProductoResponse actualizar(@PathVariable Long id,
                                       @Valid @RequestBody ProductoRequest request) {
        return productoService.actualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('" + PermisoCodigo.CATALOGO_GESTIONAR + "')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Desactiva lógicamente un producto")
    public void eliminar(@PathVariable Long id) {
        productoService.eliminar(id);
    }
}