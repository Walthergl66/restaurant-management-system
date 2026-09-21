package com.restaurante.mesas.web;

import com.restaurante.mesas.application.MesaService;
import com.restaurante.mesas.web.dto.CambiarEstadoMesaRequest;
import com.restaurante.mesas.web.dto.MesaRequest;
import com.restaurante.mesas.web.dto.MesaResponse;
import com.restaurante.usuarios.PermisoCodigo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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
 * Mesas del salón y su estado.
 */
@RestController
@RequestMapping("/api/v1/mesas")
@Tag(name = "mesas", description = "Mesas del salón")
public class MesaController {

    private final MesaService mesaService;

    public MesaController(MesaService mesaService) {
        this.mesaService = mesaService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('" + PermisoCodigo.MESAS_VER + "')")
    @Operation(summary = "Lista mesas")
    public List<MesaResponse> listar(@RequestParam(defaultValue = "true") boolean soloActivas) {
        return mesaService.listar(soloActivas);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('" + PermisoCodigo.MESAS_VER + "')")
    @Operation(summary = "Obtiene una mesa por id")
    public MesaResponse obtener(@PathVariable Long id) {
        return mesaService.obtener(id);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('" + PermisoCodigo.MESAS_GESTIONAR + "')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crea una mesa")
    public MesaResponse crear(@Valid @RequestBody MesaRequest request) {
        return mesaService.crear(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('" + PermisoCodigo.MESAS_GESTIONAR + "')")
    @Operation(summary = "Actualiza número, capacidad o ubicación")
    public MesaResponse actualizar(@PathVariable Long id,
                                   @Valid @RequestBody MesaRequest request) {
        return mesaService.actualizar(id, request);
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasAuthority('" + PermisoCodigo.MESAS_GESTIONAR + "')")
    @Operation(summary = "Cambia el estado de una mesa (uso administrativo)")
    public MesaResponse cambiarEstado(@PathVariable Long id,
                                      @Valid @RequestBody CambiarEstadoMesaRequest request) {
        return mesaService.cambiarEstado(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('" + PermisoCodigo.MESAS_GESTIONAR + "')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Desactiva lógicamente una mesa")
    public void eliminar(@PathVariable Long id) {
        mesaService.eliminar(id);
    }
}