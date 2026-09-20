package com.restaurante.configuracion.web;

import com.restaurante.configuracion.application.ImpresoraService;
import com.restaurante.configuracion.web.dto.ImpresoraRequest;
import com.restaurante.configuracion.web.dto.ImpresoraResponse;
import com.restaurante.usuarios.PermisoCodigo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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

/**
 * Administración de impresoras térmicas. Solo administración.
 */
@RestController
@RequestMapping("/api/v1/impresoras")
@Tag(name = "configuracion", description = "Impresoras térmicas del local")
public class ImpresoraController {

    private final ImpresoraService impresoraService;

    public ImpresoraController(ImpresoraService impresoraService) {
        this.impresoraService = impresoraService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('" + PermisoCodigo.CONFIGURACION_GESTIONAR + "')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crea una impresora")
    public ImpresoraResponse crear(@Valid @RequestBody ImpresoraRequest request) {
        return impresoraService.crear(request);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('" + PermisoCodigo.CONFIGURACION_GESTIONAR + "')")
    @Operation(summary = "Lista paginada de impresoras")
    public Page<ImpresoraResponse> listar(
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanio) {
        return impresoraService.listar(
                PageRequest.of(pagina, tamanio, Sort.by(Sort.Direction.ASC, "id")));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('" + PermisoCodigo.CONFIGURACION_GESTIONAR + "')")
    @Operation(summary = "Obtiene una impresora por id")
    public ImpresoraResponse obtener(@PathVariable Long id) {
        return impresoraService.obtener(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('" + PermisoCodigo.CONFIGURACION_GESTIONAR + "')")
    @Operation(summary = "Actualiza una impresora")
    public ImpresoraResponse actualizar(@PathVariable Long id,
                                        @Valid @RequestBody ImpresoraRequest request) {
        return impresoraService.actualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('" + PermisoCodigo.CONFIGURACION_GESTIONAR + "')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Desactiva lógicamente una impresora")
    public void eliminar(@PathVariable Long id) {
        impresoraService.eliminar(id);
    }
}