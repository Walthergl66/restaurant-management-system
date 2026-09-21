package com.restaurante.caja.application;

import com.restaurante.caja.CajaResumen;
import com.restaurante.caja.Cajas;
import com.restaurante.caja.Cajas.MovimientoRegistro;
import com.restaurante.caja.domain.Caja;
import com.restaurante.caja.domain.EstadoCaja;
import com.restaurante.caja.domain.MovimientoCaja;
import com.restaurante.caja.infrastructure.CajaRepository;
import com.restaurante.caja.infrastructure.MovimientoCajaRepository;
import com.restaurante.caja.web.dto.CajaResponse;
import com.restaurante.caja.web.dto.MovimientoResponse;
import com.restaurante.shared.domain.Money;
import com.restaurante.shared.domain.exception.BusinessRuleException;
import com.restaurante.shared.domain.exception.NotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Caja del establecimiento (RF-32 a RF-35): apertura, movimientos (ingresos
 * por cobros y egresos manuales) y cierre conciliado con diferencia. Una sola
 * caja abierta a la vez (índice único parcial en la base) y {@code @Version}
 * optimista contra cierres simultáneos.
 */
@Service
@Transactional
public class CajaService implements Cajas {

    private final CajaRepository cajaRepository;
    private final MovimientoCajaRepository movimientoRepository;

    public CajaService(CajaRepository cajaRepository, MovimientoCajaRepository movimientoRepository) {
        this.cajaRepository = cajaRepository;
        this.movimientoRepository = movimientoRepository;
    }

    public CajaResponse apertura(Money aperturaInicial) {
        cajaRepository.findByEstado(EstadoCaja.ABIERTA).ifPresent(c -> {
            throw new BusinessRuleException("Ya existe una caja abierta (id " + c.getId() + ")");
        });
        Caja caja = cajaRepository.save(new Caja(aperturaInicial, usuarioActual()));
        return CajaResponse.from(caja);
    }

    public MovimientoResponse egreso(Long cajaId, String concepto, Money monto, String metodo) {
        Caja caja = cargarAbierta(cajaId);
        MovimientoCaja movimiento = movimientoRepository.save(new MovimientoCaja(caja.getId(), "EGRESO", concepto, monto, metodo, null));
        return tipoMovimiento(movimiento);
    }

    public CajaResponse cierre(Long id, Money montoReal) {
        Caja caja = cargar(id);
        Money esperado = calcularEsperado(caja);
        caja.cerrar(montoReal, esperado, usuarioActual());
        cajaRepository.save(caja);
        return CajaResponse.from(caja);
    }

    @Transactional(readOnly = true)
    public List<CajaResponse> listar() {
        return cajaRepository.findAllByOrderByAbiertaAtDesc().stream()
                .map(CajaResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public CajaResponse detalle(Long id) {
        return CajaResponse.from(cargar(id));
    }

    @Transactional(readOnly = true)
    public List<MovimientoResponse> movimientos(Long cajaId) {
        return movimientoRepository.findByCajaId(cajaId).stream()
                .map(this::tipoMovimiento)
                .toList();
    }

    @Override
    public Optional<CajaResumen> cajaAbierta() {
        return cajaRepository.findByEstado(EstadoCaja.ABIERTA)
                .map(this::resumen);
    }

    public void registrarIngreso(Long cajaId, String concepto, Money monto, String metodo, Long pagoId) {
        cargarAbierta(cajaId);
        movimientoRepository.save(new MovimientoCaja(cajaId, "INGRESO", concepto, monto, metodo, pagoId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<MovimientoRegistro> movimientosEnPeriodo(Instant desde, Instant hasta) {
        return movimientoRepository.findByCreatedAtBetween(desde, hasta).stream()
                .map(m -> new MovimientoRegistro(m.getTipo(), m.getMetodo(),
                        m.getMonto().getAmount(), m.getPagoId(), m.getCreatedAt()))
                .toList();
    }

    @Override
    public void registrarIngreso(Long cajaId, String concepto, java.math.BigDecimal monto,
                                 String metodo, Long pagoId) {
        registrarIngreso(cajaId, concepto, Money.of(monto), metodo, pagoId);
    }

    private boolean esIngreso(MovimientoCaja m) {
        return "INGRESO".equals(m.getTipo());
    }

    private Money esperadoDe(List<MovimientoCaja> movimientos) {
        Money suma = Money.ZERO;
        for (MovimientoCaja m : movimientos) {
            suma = esIngreso(m) ? suma.add(m.getMonto()) : suma.subtract(m.getMonto());
        }
        return suma;
    }

    private Money calcularEsperado(Caja caja) {
        Money esperado = esperadoDe(movimientoRepository.findByCajaId(caja.getId()));
        return Money.ZERO.add(caja.getAperturaInicial()).add(esperado);
    }

    private Caja cargar(Long id) {
        return cajaRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Caja " + id + " no encontrada"));
    }

    private Caja cargarAbierta(Long id) {
        Caja caja = cargar(id);
        if (caja.getEstado() != EstadoCaja.ABIERTA) {
            throw new BusinessRuleException("La caja " + id + " está cerrada");
        }
        return caja;
    }

    private CajaResumen resumen(Caja caja) {
        return new CajaResumen(
                caja.getId(),
                caja.getEstado().name(),
                caja.getAperturaInicial().getAmount(),
                caja.getCierreEsperado() == null ? null : caja.getCierreEsperado().getAmount(),
                caja.getCierreReal() == null ? null : caja.getCierreReal().getAmount(),
                caja.getDiferencia() == null ? null : caja.getDiferencia().getAmount(),
                caja.getAbiertaPor(),
                caja.getCerradaPor(),
                caja.getAbiertaAt(),
                caja.getCerradaAt());
    }

    private MovimientoResponse tipoMovimiento(MovimientoCaja m) {
        return new MovimientoResponse(
                m.getId(), m.getCajaId(), m.getTipo(), m.getConcepto(),
                m.getMonto().getAmount(), m.getMetodo(), m.getPagoId(), m.getCreatedAt());
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