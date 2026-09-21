package com.restaurante.caja.web.dto;

import com.restaurante.caja.CajaResumen;
import com.restaurante.caja.domain.Caja;

import java.math.BigDecimal;
import java.time.Instant;

public record CajaResponse(
        Long id,
        String estado,
        BigDecimal aperturaInicial,
        BigDecimal cierreEsperado,
        BigDecimal cierreReal,
        BigDecimal diferencia,
        Long version,
        String abiertaPor,
        String cerradaPor,
        Instant abiertaAt,
        Instant cerradaAt) {

    public static CajaResponse from(Caja caja) {
        return new CajaResponse(
                caja.getId(),
                caja.getEstado().name(),
                caja.getAperturaInicial().getAmount(),
                caja.getCierreEsperado() == null ? null : caja.getCierreEsperado().getAmount(),
                caja.getCierreReal() == null ? null : caja.getCierreReal().getAmount(),
                caja.getDiferencia() == null ? null : caja.getDiferencia().getAmount(),
                caja.getVersion(),
                caja.getAbiertaPor(),
                caja.getCerradaPor(),
                caja.getAbiertaAt(),
                caja.getCerradaAt());
    }

    public static CajaResponse from(CajaResumen resumen) {
        return new CajaResponse(
                resumen.id(), resumen.estado(), resumen.aperturaInicial(),
                resumen.cierreEsperado(), resumen.cierreReal(), resumen.diferencia(),
                null, resumen.abiertaPor(), resumen.cerradaPor(),
                resumen.abiertaAt(), resumen.cerradaAt());
    }
}