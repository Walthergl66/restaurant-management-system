package com.restaurante.clientes.domain;

import com.restaurante.shared.domain.Money;

import java.math.BigDecimal;
import java.util.List;

/** RF-40/RF-41: línea congelada del carrito/pedido del cliente.
 *  El precio se congela al confirmar (Money SPI, nunca double). */
public class LineaPedidoClienteTabletLinea {

    private final Long productoId;
    private final String nombre;
    private final Money precio;
    private final int cantidad;
    private final List<Money> extrasPrecios;
    private final String observaciones;

    public LineaPedidoClienteTabletLinea(Long productoId, String nombre, Money precio, int cantidad,
                                          List<Money> extrasPrecios, String observaciones) {
        if (productoId == null) throw new IllegalArgumentException("productoId obligatorio");
        if (precio == null) throw new IllegalArgumentException("precio obligatorio");
        if (cantidad <= 0) throw new IllegalArgumentException("cantidad >0");
        this.productoId = productoId;
        this.nombre = nombre;
        this.precio = precio;
        this.cantidad = cantidad;
        this.extrasPrecios = extrasPrecios == null ? List.of() : List.copyOf(extrasPrecios);
        this.observaciones = observaciones;
    }

    public Money subtotal() {
        Money base = precio.multiply(BigDecimal.valueOf(cantidad));
        Money extrasTotal = extrasPrecios.stream().reduce(Money.ZERO, (a, b) -> a.add(b.multiply(BigDecimal.valueOf(cantidad))));
        // Cada extra se multiplica por cantidad (como en pedidos normales)
        return base.add(extrasTotal);
    }

    public Long productoId() { return productoId; }
    public String nombre() { return nombre; }
    public Money precio() { return precio; }
    public int cantidad() { return cantidad; }
    public List<Money> extrasPrecios() { return extrasPrecios; }
    public String observaciones() { return observaciones; }
}
