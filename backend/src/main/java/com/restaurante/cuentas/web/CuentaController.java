package com.restaurante.cuentas.web;

import com.restaurante.cuentas.application.CuentaService;
import com.restaurante.cuentas.domain.EstadoCuenta;
import com.restaurante.cuentas.web.dto.AdicionRequest;
import com.restaurante.cuentas.web.dto.AdicionResponse;
import com.restaurante.cuentas.web.dto.CuentaResponse;
import com.restaurante.usuarios.PermisoCodigo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Cuentas de mesa: consumo, total (RNF-16), adiciones y cierre (RF-24, RF-25).
 */
@RestController
@RequestMapping("/api/v1/cuentas")
@Tag(name = "cuentas", description = "Cuentas de mesa")
public class CuentaController {

    private final CuentaService cuentaService;

    public CuentaController(CuentaService cuentaService) {
        this.cuentaService = cuentaService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('" + PermisoCodigo.CUENTAS_VER + "')")
    @Operation(summary = "Lista cuentas (por mesa o estado)")
    public List<CuentaResponse> listar(@RequestParam(required = false) Long mesaId,
                                       @RequestParam(required = false) EstadoCuenta estado) {
        return cuentaService.listar(mesaId, estado);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('" + PermisoCodigo.CUENTAS_VER + "')")
    @Operation(summary = "Detalle de la cuenta: consumo, anulaciones y total calculado")
    public CuentaResponse detalle(@PathVariable Long id) {
        return cuentaService.detalle(id);
    }

    @PostMapping("/{id}/adiciones")
    @PreAuthorize("hasAuthority('" + PermisoCodigo.CUENTAS_GESTIONAR + "')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Adición: crea un borrador nuevo ligado a la misma cuenta")
    public AdicionResponse adiciones(@PathVariable Long id, @Valid @RequestBody AdicionRequest request) {
        return cuentaService.adiciones(id, request);
    }

    @PatchMapping("/{id}/cerrar")
    @PreAuthorize("hasAuthority('" + PermisoCodigo.CUENTAS_GESTIONAR + "')")
    @Operation(summary = "Cierra la cuenta y libera la mesa")
    public CuentaResponse cerrar(@PathVariable Long id) {
        return cuentaService.cerrar(id);
    }
}