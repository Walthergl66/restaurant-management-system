package com.restaurante.configuracion.web;

import com.restaurante.configuracion.application.ParametroService;
import com.restaurante.configuracion.web.dto.ParametroRequest;
import com.restaurante.configuracion.web.dto.ParametroResponse;
import com.restaurante.usuarios.PermisoCodigo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Parámetros del restaurante. Solo administración.
 */
@RestController
@RequestMapping("/api/v1/configuracion")
@Tag(name = "configuracion", description = "Parámetros del restaurante")
public class ParametroController {

    private final ParametroService parametroService;

    public ParametroController(ParametroService parametroService) {
        this.parametroService = parametroService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('" + PermisoCodigo.CONFIGURACION_GESTIONAR + "')")
    @Operation(summary = "Lista todos los parámetros")
    public List<ParametroResponse> listar() {
        return parametroService.listar();
    }

    @GetMapping("/{clave}")
    @PreAuthorize("hasAuthority('" + PermisoCodigo.CONFIGURACION_GESTIONAR + "')")
    @Operation(summary = "Obtiene un parámetro por su clave")
    public ParametroResponse obtener(@PathVariable String clave) {
        return parametroService.obtenerPorClave(clave);
    }

    @PutMapping("/{clave}")
    @PreAuthorize("hasAuthority('" + PermisoCodigo.CONFIGURACION_GESTIONAR + "')")
    @Operation(summary = "Actualiza el valor de un parámetro")
    public ParametroResponse actualizar(@PathVariable String clave,
                                        @Valid @RequestBody ParametroRequest request) {
        return parametroService.actualizar(clave, request.valor());
    }
}