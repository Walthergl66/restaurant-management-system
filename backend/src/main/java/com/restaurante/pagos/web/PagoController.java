package com.restaurante.pagos.web;

import com.restaurante.pagos.application.PagoService;
import com.restaurante.pagos.web.dto.CobroItemResponse;
import com.restaurante.pagos.web.dto.CobroRequest;
import com.restaurante.pagos.web.dto.CobroResponse;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Cobro de cuentas (RF-26, RF-27, RF-31). Al cobrar se cierra la cuenta, se
 * registran los pagos en la caja abierta y se emite el comprobante.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "pagos", description = "Cobro de cuentas")
public class PagoController {

    private final PagoService pagoService;

    public PagoController(PagoService pagoService) {
        this.pagoService = pagoService;
    }

    @PostMapping("/cuentas/{cuentaId}/cobros")
    @PreAuthorize("hasAuthority('" + PermisoCodigo.PAGOS_COBRAR + "')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cobra una cuenta (pago mixto), emite comprobante y cierra la mesa")
    public CobroResponse cobrar(@PathVariable Long cuentaId,
                                @Valid @RequestBody CobroRequest request) {
        return pagoService.cobrar(cuentaId, request.pagos(), request.documento());
    }

    @GetMapping("/pagos")
    @PreAuthorize("hasAuthority('" + PermisoCodigo.PAGOS_COBRAR + "')")
    @Operation(summary = "Pagos de una cuenta")
    public List<CobroItemResponse> cobrosDeCuenta(@RequestParam Long cuentaId) {
        return pagoService.cobrosDeCuenta(cuentaId);
    }

    @GetMapping("/pagos/{id}")
    @PreAuthorize("hasAuthority('" + PermisoCodigo.PAGOS_COBRAR + "')")
    @Operation(summary = "Detalle de un pago")
    public CobroItemResponse cobro(@PathVariable Long id) {
        return pagoService.cobro(id);
    }
}