package com.restaurante.reportes.application;

import com.restaurante.anulaciones.Anulaciones;
import com.restaurante.anulaciones.Anulaciones.DescuentoProducto;
import com.restaurante.pagos.Pagos;
import com.restaurante.pagos.Pagos.PagoRegistro;
import com.restaurante.pedidos.Pedidos;
import com.restaurante.pedidos.Pedidos.VentaProducto;
import com.restaurante.shared.domain.Money;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Reportes de ventas (RF-51): total cobrado, número de ventas, desglose por
 * método de pago, por día y por producto (ventas menos anulaciones aprobadas).
 * Todo desde registros confirmados, con redondeo en un solo lugar.
 */
@Service
@Transactional(readOnly = true)
public class ReportesService {

    private final Pagos pagos;
    private final Pedidos pedidos;
    private final Anulaciones anulaciones;

    public ReportesService(Pagos pagos, Pedidos pedidos, Anulaciones anulaciones) {
        this.pagos = pagos;
        this.pedidos = pedidos;
        this.anulaciones = anulaciones;
    }

    public ReporteVentas ventas(Instant desde, Instant hasta) {
        List<PagoRegistro> registros = pagos.pagosEnPeriodo(desde, hasta);

        BigDecimal total = BigDecimal.ZERO;
        Set<Long> cuentas = new HashSet<>();
        Map<String, BigDecimal> porMetodo = new LinkedHashMap<>();
        Map<String, BigDecimal> porDia = new LinkedHashMap<>();
        for (PagoRegistro pago : registros) {
            total = total.add(pago.monto());
            cuentas.add(pago.cuentaId());
            porMetodo.merge(pago.metodo(), pago.monto(), BigDecimal::add);
            porDia.merge(pago.fecha().truncatedTo(java.time.temporal.ChronoUnit.DAYS).toString(),
                    pago.monto(), BigDecimal::add);
        }

        Map<String, ProductoVenta> productos = new LinkedHashMap<>();
        for (VentaProducto venta : pedidos.ventasPorProducto(desde, hasta)) {
            String clave = venta.productoId() + "\u001F" + venta.nombreProducto();
            productos.put(clave, new ProductoVenta(
                    venta.productoId(), venta.nombreProducto(), venta.cantidad(), venta.monto()));
        }
        for (DescuentoProducto descuento : anulaciones.aprobadasPorProducto(desde, hasta)) {
            String clave = descuento.productoId() + "\u001F" + descuento.nombreProducto();
            ProductoVenta venta = productos.get(clave);
            if (venta != null) {
                BigDecimal restado = venta.monto().subtract(descuento.monto()).max(BigDecimal.ZERO);
                productos.put(clave, new ProductoVenta(
                        venta.productoId(), venta.nombre(),
                        Math.max(0, venta.cantidad() - descuento.cantidad()), restado));
            }
        }

        List<MetodoVenta> metodos = new ArrayList<>();
        porMetodo.forEach((metodo, monto) -> metodos.add(new MetodoVenta(metodo, monto)));
        List<VentaDia> dias = new ArrayList<>();
        porDia.forEach((dia, monto) -> dias.add(new VentaDia(dia, monto)));

        return new ReporteVentas(
                Money.round(total),
                cuentas.size(),
                metodos.stream().map(m -> new MetodoVenta(m.metodo(), Money.round(m.total()))).toList(),
                dias.stream().map(d -> new VentaDia(d.fecha(), Money.round(d.total()))).toList(),
                productos.values().stream()
                        .map(p -> new ProductoVenta(p.productoId(), p.nombre(), p.cantidad(),
                                Money.round(p.monto())))
                        .toList());
    }

    public record ReporteVentas(
            BigDecimal total,
            int numeroVentas,
            List<MetodoVenta> porMetodo,
            List<VentaDia> porDia,
            List<ProductoVenta> porProducto) {
    }

    public record MetodoVenta(String metodo, BigDecimal total) {
    }

    public record VentaDia(String fecha, BigDecimal total) {
    }

    public record ProductoVenta(String productoId, String nombre, int cantidad, BigDecimal monto) {
    }
}