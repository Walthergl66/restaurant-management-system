package com.restaurante.cuentas.application;

import com.restaurante.anulaciones.Anulaciones;
import com.restaurante.anulaciones.AnulacionResumen;
import com.restaurante.cuentas.Cuentas;
import com.restaurante.cuentas.CuentaResumen;
import com.restaurante.cuentas.domain.CalculadoraCuenta;
import com.restaurante.cuentas.domain.Cuenta;
import com.restaurante.cuentas.domain.EstadoCuenta;
import com.restaurante.cuentas.infrastructure.CuentaRepository;
import com.restaurante.cuentas.web.dto.AdicionRequest;
import com.restaurante.cuentas.web.dto.AdicionResponse;
import com.restaurante.cuentas.web.dto.CuentaResponse;
import com.restaurante.mesas.Mesas;
import com.restaurante.pedidos.PedidoCancelado;
import com.restaurante.pedidos.PedidoCreado;
import com.restaurante.pedidos.PedidoResumen;
import com.restaurante.pedidos.Pedidos;
import com.restaurante.shared.domain.Money;
import com.restaurante.shared.domain.exception.BusinessRuleException;
import com.restaurante.shared.domain.exception.NotFoundException;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Cuentas de mesa (RF-24, RF-25): abren/reutilizan la cuenta del turno, listan
 * el consumo y el total (siempre calculado por {@link CalculadoraCuenta}),
 * permiten adiciones y cierran la cuenta librando la mesa.
 */
@Service
@Transactional
public class CuentaService implements Cuentas {

    private final CuentaRepository cuentaRepository;
    private final Pedidos pedidos;
    private final Anulaciones anulaciones;
    private final Mesas mesas;
    private final CalculadoraCuenta calculadora = new CalculadoraCuenta();

    public CuentaService(CuentaRepository cuentaRepository, Pedidos pedidos,
                         Anulaciones anulaciones, Mesas mesas) {
        this.cuentaRepository = cuentaRepository;
        this.pedidos = pedidos;
        this.anulaciones = anulaciones;
        this.mesas = mesas;
    }

    /**
     * Abre la cuenta del turno de la mesa (o reutiliza la abierta). Idempotente.
     */
    @EventListener
    public void alCrearPedido(PedidoCreado evento) {
        abrirParaMesa(evento.mesaId());
    }

    /**
     * Si al cancelar un borrador la mesa se queda sin pedidos, se cierra la
     * cuenta vacía (la mesa ya la liberó el módulo de pedidos).
     */
    @EventListener
    public void alCancelarPedido(PedidoCancelado evento) {
        if (pedidos.pedidosDeMesa(evento.mesaId()).isEmpty()) {
            cerrarAbiertaDeMesa(evento.mesaId());
        }
    }

    private void abrirParaMesa(Long mesaId) {
        if (cuentaRepository.findByMesaIdAndEstado(mesaId, EstadoCuenta.ABIERTA).isEmpty()) {
            cuentaRepository.save(new Cuenta(mesaId));
        }
    }

    private void cerrarAbiertaDeMesa(Long mesaId) {
        cuentaRepository.findByMesaIdAndEstado(mesaId, EstadoCuenta.ABIERTA)
                .ifPresent(Cuenta::cerrar);
    }

    @Transactional(readOnly = true)
    public List<CuentaResponse> listar(Long mesaId, EstadoCuenta estado) {
        List<Cuenta> cuentas;
        if (mesaId != null) {
            cuentas = cuentaRepository.findByMesaId(mesaId);
        } else if (estado != null) {
            cuentas = cuentaRepository.findByEstado(estado);
        } else {
            cuentas = cuentaRepository.findAll();
        }
        return cuentas.stream()
                .map(this::aRespuesta)
                .toList();
    }

    @Transactional(readOnly = true)
    public CuentaResponse detalle(Long id) {
        return aRespuesta(cargar(id));
    }

    /**
     * Cierra la cuenta y libera la mesa. No admite cerrar con borradores
     * pendientes sin confirmar (la cuenta solo totaliza confirmados).
     */
    public CuentaResponse cerrar(Long id) {
        return aRespuesta(cerrarValidando(id));
    }

    @Override
    public void cerrarParaCobro(Long cuentaId) {
        cerrarValidando(cuentaId);
    }

    private Cuenta cerrarValidando(Long id) {
        Cuenta cuenta = cargar(id);
        boolean hayBorradores = pedidos.pedidosDeMesa(cuenta.getMesaId()).stream()
                .anyMatch(p -> "BORRADOR".equals(p.estado()));
        if (hayBorradores) {
            throw new BusinessRuleException(
                    "No se puede cerrar la cuenta con pedidos en borrador sin confirmar");
        }
        cuenta.cerrar();
        mesas.liberarMesa(cuenta.getMesaId());
        return cuenta;
    }

    /**
     * Adición: crea un borrador nuevo ligado a la misma cuenta (RF-17 a RF-19).
     */
    public AdicionResponse adiciones(Long id, AdicionRequest request) {
        Cuenta cuenta = cargar(id);
        if (cuenta.getEstado() != EstadoCuenta.ABIERTA) {
            throw new BusinessRuleException("La cuenta está cerrada; no admite adiciones");
        }
        String codigo = pedidos.crearAdicion(cuenta.getMesaId(), request.codigo(), request.notas());
        return new AdicionResponse(codigo, cuenta.getId());
    }

    @Override
    public CuentaResumen resumen(Long cuentaId) {
        CuentaResponse detalle = detalle(cuentaId);
        return new CuentaResumen(
                detalle.id(),
                detalle.mesaId(),
                detalle.numeroMesa(),
                detalle.estado().name(),
                detalle.total());
    }

    private CuentaResponse aRespuesta(Cuenta cuenta) {
        List<PedidoResumen> pedidosQueCuentan = pedidos.pedidosConfirmadosDeMesa(cuenta.getMesaId());
        List<String> codigos = pedidosQueCuentan.stream().map(PedidoResumen::codigo).toList();
        List<AnulacionResumen> aprobadas = anulaciones.aprobadasDe(codigos);
        Money total = calculadora.total(pedidosQueCuentan, aprobadas);
        int numeroMesa = mesas.mesa(cuenta.getMesaId())
                .map(m -> m.numero())
                .orElse(0);
        return CuentaResponse.from(cuenta, numeroMesa, pedidosQueCuentan, aprobadas, total);
    }

    private Cuenta cargar(Long id) {
        return cuentaRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Cuenta " + id + " no encontrada"));
    }
}