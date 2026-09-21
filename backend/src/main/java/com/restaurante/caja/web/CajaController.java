package com.restaurante.caja.web;

import com.restaurante.caja.application.CajaService;
import com.restaurante.caja.web.dto.AperturaRequest;
import com.restaurante.caja.web.dto.CajaResponse;
import com.restaurante.caja.web.dto.CierreRequest;
import com.restaurante.caja.web.dto.EgresoRequest;
import com.restaurante.caja.web.dto.MovimientoResponse;
import com.restaurante.shared.domain.Money;
import com.restaurante.usuarios.PermisoCodigo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Caja: apertura de turno, movimientos (ingresos por cobros y egresos) y cierre
 * conciliado con diferencias (RF-32 a RF-35).
 */
@RestController
@RequestMapping("/api/v1/cajas")
@Tag(name = "caja", description = "Caja y turnos")
public class CajaController {

    private final CajaService cajaService;

    public CajaController(CajaService cajaService) {
        this.cajaService = cajaService;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('" + PermisoCodigo.CAJA_CIERRE + "', '" + PermisoCodigo.FINANZAS_VER + "')")
    @Operation(summary = "Lista cajas (turnos)")
    public List<CajaResponse> listar() {
        return cajaService.listar();
    }

    @GetMapping("/abierta")
    @PreAuthorize("hasAnyAuthority('" + PermisoCodigo.CAJA_CIERRE + "', '" + PermisoCodigo.CAJA_MOVIMIENTOS + "')")
    @Operation(summary = "Caja abierta actual (404 si no hay turno abierto)")
    public CajaResponse abierta() {
        return cajaService.cajaAbierta()
                .map(CajaResponse::from)
                .orElseThrow(() -> new com.restaurante.shared.domain.exception.NotFoundException("No hay caja abierta"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('" + PermisoCodigo.CAJA_CIERRE + "', '" + PermisoCodigo.FINANZAS_VER + "')")
    @Operation(summary = "Detalle de una caja")
    public CajaResponse detalle(@PathVariable Long id) {
        return cajaService.detalle(id);
    }

    @GetMapping("/{id}/movimientos")
    @PreAuthorize("hasAuthority('" + PermisoCodigo.CAJA_MOVIMIENTOS + "')")
    @Operation(summary = "Movimientos de una caja")
    public List<MovimientoResponse> movimientos(@PathVariable Long id) {
        return cajaService.movimientos(id);
    }

    @PostMapping("/apertura")
    @PreAuthorize("hasAuthority('" + PermisoCodigo.CAJA_APERTURA + "')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Abre una caja (solo una a la vez)")
    public CajaResponse apertura(@Valid @RequestBody AperturaRequest request) {
        return cajaService.apertura(Money.of(request.montoInicial()));
    }

    @PostMapping("/{id}/egresos")
    @PreAuthorize("hasAuthority('" + PermisoCodigo.CAJA_MOVIMIENTOS + "')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registra un egreso manual en la caja abierta")
    public MovimientoResponse egreso(@PathVariable Long id, @Valid @RequestBody EgresoRequest request) {
        return cajaService.egreso(id, request.concepto(), Money.of(request.monto()), request.metodo());
    }

    @PostMapping("/{id}/cierre")
    @PreAuthorize("hasAuthority('" + PermisoCodigo.CAJA_CIERRE + "')")
    @Operation(summary = "Cierra la caja conciliando el monto real")
    public CajaResponse cierre(@PathVariable Long id, @Valid @RequestBody CierreRequest request) {
        return cajaService.cierre(id, Money.of(request.montoReal()));
    }
}