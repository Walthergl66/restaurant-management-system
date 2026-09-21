package com.restaurante.pagos.application;

import com.restaurante.caja.CajaResumen;
import com.restaurante.caja.Cajas;
import com.restaurante.cuentas.CuentaResumen;
import com.restaurante.cuentas.Cuentas;
import com.restaurante.facturacion.ComprobanteResumen;
import com.restaurante.facturacion.EmisionComprobantes;
import com.restaurante.facturacion.EmitirComprobante;
import com.restaurante.pagos.CuentaCobrada;
import com.restaurante.pagos.Pagos;
import com.restaurante.pagos.Pagos.PagoRegistro;
import com.restaurante.pagos.domain.MetodoPago;
import com.restaurante.pagos.domain.Pago;
import com.restaurante.pagos.infrastructure.PagoRepository;
import com.restaurante.pagos.web.dto.CobroDocumentoRequest;
import com.restaurante.pagos.web.dto.CobroItemRequest;
import com.restaurante.pagos.web.dto.CobroItemResponse;
import com.restaurante.pagos.web.dto.CobroResponse;
import com.restaurante.shared.domain.Money;
import com.restaurante.shared.domain.exception.BusinessRuleException;
import com.restaurante.shared.domain.exception.NotFoundException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Cobro de cuentas (RF-26, RF-27, RF-31). El cobro es mixto: una lista de
 * pagos (uno por método) cuya suma debe cubrir exactamente el total de la
 * cuenta. En una sola transacción registra los pagos, los ingresos en la caja
 * abierta, emite el comprobante y cierra la cuenta (liberando la mesa).
 */
@Service
@Transactional
public class PagoService implements Pagos {

    private final PagoRepository pagoRepository;
    private final Cuentas cuentas;
    private final Cajas cajas;
    private final EmisionComprobantes emisionComprobantes;
    private final ApplicationEventPublisher eventPublisher;

    public PagoService(PagoRepository pagoRepository, Cuentas cuentas, Cajas cajas,
                       EmisionComprobantes emisionComprobantes,
                       ApplicationEventPublisher eventPublisher) {
        this.pagoRepository = pagoRepository;
        this.cuentas = cuentas;
        this.cajas = cajas;
        this.emisionComprobantes = emisionComprobantes;
        this.eventPublisher = eventPublisher;
    }

    public CobroResponse cobrar(Long cuentaId, List<CobroItemRequest> cobros,
                                CobroDocumentoRequest documento) {
        CuentaResumen cuenta = cuentas.resumen(cuentaId);
        if (!"ABIERTA".equals(cuenta.estado())) {
            throw new BusinessRuleException("La cuenta " + cuentaId + " no está abierta para cobrar");
        }

        CajaResumen caja = cajas.cajaAbierta()
                .orElseThrow(() -> new BusinessRuleException(
                        "No hay una caja abierta; abra caja antes de cobrar"));

        Money total = Money.of(cuenta.total());
        validarCobros(cobros, total);

        String usuario = usuarioActual();
        List<CobroItemResponse> registrados = new ArrayList<>();
        for (CobroItemRequest cobro : cobros) {
            Pago pago = pagoRepository.save(new Pago(
                    cuentaId, caja.id(), cobro.metodo(), Money.of(cobro.monto()), usuario));
            cajas.registrarIngreso(caja.id(), "Cobro cuenta " + cuentaId,
                    cobro.monto(), cobro.metodo().name(), pago.getId());
            registrados.add(new CobroItemResponse(pago.getId(), cobro.metodo().name(), cobro.monto()));
        }

        ComprobanteResumen comprobante = emisionComprobantes.emitir(new EmitirComprobante(
                cuentaId,
                documento == null || documento.tipo() == null ? "TICKET" : documento.tipo(),
                total.getAmount(),
                documento == null ? null : documento.clienteNombre(),
                documento == null ? null : documento.clienteIdentificacion(),
                usuario));

        cuentas.cerrarParaCobro(cuentaId);

        eventPublisher.publishEvent(new CuentaCobrada(
                cuentaId, cuenta.mesaId(), total.getAmount(), caja.id(),
                comprobante.correlativo(),
                registrados.stream()
                        .map(r -> new CuentaCobrada.PagoResumen(r.id(), r.metodo(), r.monto()))
                        .toList()));

        return new CobroResponse(cuentaId, caja.id(), total.getAmount(),
                comprobante.correlativo(), registrados);
    }

    private void validarCobros(List<CobroItemRequest> cobros, Money total) {
        if (cobros == null || cobros.isEmpty()) {
            throw new BusinessRuleException("El cobro debe incluir al menos un pago");
        }
        Money suma = Money.ZERO;
        for (CobroItemRequest cobro : cobros) {
            if (cobro.metodo() == null) {
                throw new BusinessRuleException("El método de pago es obligatorio");
            }
            suma = suma.add(Money.of(cobro.monto()));
        }
        if (suma.compareTo(total) != 0) {
            throw new BusinessRuleException(
                    "El total de pagos (" + suma + ") no coincide con el total de la cuenta (" + total + ")");
        }
    }

    @Transactional(readOnly = true)
    public List<CobroItemResponse> cobrosDeCuenta(Long cuentaId) {
        return pagoRepository.findByCuentaId(cuentaId).stream()
                .map(p -> new CobroItemResponse(p.getId(), p.getMetodo().name(), p.getMonto().getAmount()))
                .toList();
    }

    @Transactional(readOnly = true)
    public CobroItemResponse cobro(Long id) {
        Pago pago = pagoRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Pago " + id + " no encontrado"));
        return new CobroItemResponse(pago.getId(), pago.getMetodo().name(), pago.getMonto().getAmount());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PagoRegistro> pagosEnPeriodo(Instant desde, Instant hasta) {
        return pagoRepository.findByCreatedAtBetween(desde, hasta).stream()
                .map(p -> new PagoRegistro(p.getCuentaId(), p.getMetodo().name(),
                        p.getMonto().getAmount(), p.getCreatedAt()))
                .toList();
    }

    private String usuarioActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null
                || "anonymousUser".equals(auth.getPrincipal())) {
            return "sistema";
        }
        return auth.getName();
    }
}