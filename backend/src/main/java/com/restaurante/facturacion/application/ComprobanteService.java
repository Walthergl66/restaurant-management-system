package com.restaurante.facturacion.application;

import com.restaurante.facturacion.ComprobanteResumen;
import com.restaurante.facturacion.EmisorInterno;
import com.restaurante.facturacion.EmisionComprobantes;
import com.restaurante.facturacion.EmitirComprobante;
import com.restaurante.facturacion.domain.Comprobante;
import com.restaurante.facturacion.infrastructure.ComprobanteRepository;
import com.restaurante.shared.domain.Money;
import com.restaurante.shared.domain.exception.BusinessRuleException;
import com.restaurante.shared.domain.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Emisión y consulta de comprobantes. La numeración secuencial la resuelve el
 * emisor interno (interfaz {@link EmisionComprobantes}); cuando llegue la
 * facturación electrónica del SRI se agrega otra implementación sin tocar el
 * cobro (RF-30).
 */
@Service
@Transactional
public class ComprobanteService implements EmisionComprobantes {

    private final ComprobanteRepository comprobanteRepository;
    private final EmisorInterno emisor;

    public ComprobanteService(ComprobanteRepository comprobanteRepository, EmisorInterno emisor) {
        this.comprobanteRepository = comprobanteRepository;
        this.emisor = emisor;
    }

    @Override
    public ComprobanteResumen emitir(EmitirComprobante command) {
        if (!"FACTURA".equals(command.tipo()) && !"TICKET".equals(command.tipo())) {
            throw new BusinessRuleException("Tipo de comprobante inválido: " + command.tipo());
        }
        if (Money.of(command.total()).isNegative()) {
            throw new BusinessRuleException("El total del comprobante no puede ser negativo");
        }
        Long secuencial = comprobanteRepository.siguienteSecuencial();
        Comprobante comprobante = emisor.emitir(command, secuencial);
        comprobanteRepository.save(comprobante);
        return resumen(comprobante);
    }

    @Transactional(readOnly = true)
    public ComprobanteResumen obtener(Long id) {
        Comprobante comprobante = comprobanteRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Comprobante " + id + " no encontrado"));
        return resumen(comprobante);
    }

    @Transactional(readOnly = true)
    public List<ComprobanteResumen> listar(Long cuentaId, Instant desde, Instant hasta) {
        List<Comprobante> comprobantes;
        if (cuentaId != null) {
            comprobantes = comprobanteRepository.findByCuentaId(cuentaId);
        } else if (desde != null || hasta != null) {
            comprobantes = comprobanteRepository.findByFechaBetween(
                    desde == null ? Instant.EPOCH : desde,
                    hasta == null ? Instant.now() : hasta);
        } else {
            comprobantes = comprobanteRepository.findAll();
        }
        return comprobantes.stream().map(this::resumen).toList();
    }

    private ComprobanteResumen resumen(Comprobante c) {
        return new ComprobanteResumen(
                c.getId(), c.getCorrelativo(), c.getSecuencial(), c.getCuentaId(),
                c.getTipo(), c.getTotal(), c.getClienteNombre(),
                c.getClienteIdentificacion(), c.getEmitidoPor(), c.getFecha());
    }
}