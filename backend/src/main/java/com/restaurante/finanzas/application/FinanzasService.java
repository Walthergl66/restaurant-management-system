package com.restaurante.finanzas.application;

import com.restaurante.caja.Cajas;
import com.restaurante.caja.Cajas.MovimientoRegistro;
import com.restaurante.pagos.Pagos;
import com.restaurante.pagos.Pagos.PagoRegistro;
import com.restaurante.shared.domain.Money;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Resumen financiero por período (RF-36 a RF-39): ingresos son los cobros
 * (pagos registrados), egresos los movimientos manuales de caja tipo EGRESO y
 * el resultado la diferencia. Todo se lee desde los registros, con la regla
 * de redondeo única en un solo lugar.
 */
@Service
@Transactional(readOnly = true)
public class FinanzasService {

    private final Pagos pagos;
    private final Cajas cajas;

    public FinanzasService(Pagos pagos, Cajas cajas) {
        this.pagos = pagos;
        this.cajas = cajas;
    }

    public Resumen resumen(Instant desde, Instant hasta) {
        BigDecimal ingresos = BigDecimal.ZERO;
        for (PagoRegistro pago : pagos.pagosEnPeriodo(desde, hasta)) {
            ingresos = ingresos.add(pago.monto());
        }

        BigDecimal egresos = BigDecimal.ZERO;
        for (MovimientoRegistro movimiento : cajas.movimientosEnPeriodo(desde, hasta)) {
            if ("EGRESO".equals(movimiento.tipo())) {
                egresos = egresos.add(movimiento.monto());
            }
        }

        BigDecimal redondeados = Money.round(ingresos);
        BigDecimal redondeadosEgreso = Money.round(egresos);
        return new Resumen(redondeados, redondeadosEgreso,
                Money.round(redondeados.subtract(redondeadosEgreso)));
    }

    public record Resumen(BigDecimal ingresos, BigDecimal egresos, BigDecimal resultado) {
    }
}