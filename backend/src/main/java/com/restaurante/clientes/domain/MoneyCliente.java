package com.restaurante.clientes.domain;

import com.restaurante.shared.domain.Money;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/** RF-41/RF-09/RF-22/RF-23/RF-41: &uacute;nica regla de dinero del m&oacute;dulo
 *  clientes. Todo dinero del cliente (carrito RF-40, total congelado RF-41,
 *  m&eacute;todo de pago RF-42, historial RF-45) pasa SIEMPRE por aqu&iacute;
 *  con el SPI Money del shared (Money.of, Money.round, Money.add) que ya
 *  existe en disco (shared/domain/Money.java) — regla &uacute;nica de
 *  redondeo en un solo lugar (RF-09/22/23/41 de la Arquitectura). Nunca
 *  double; nunca BigDecimal suelto fuera de Money. */
public record MoneyCliente(BigDecimal cantidad) implements Comparable<MoneyCliente> {

    public static final MoneyCliente ZERO = new MoneyCliente(Money.ZERO.cantidad());
    public static final MoneyCliente TASA_IVA = new MoneyCliente(new BigDecimal("0.15")); // RF-11 IVA 15%


    public MoneyCliente {
        if (cantidad == null) {
            throw new IllegalArgumentException("Money nunca null (RF-09/41)");
        }
        cantidad = Money.round(cantidad).cantidad();
    }

    public static MoneyCliente of(Money money) {
        return new MoneyCliente(money.cantidad());
    }

    public static MoneyCliente of(BigDecimal cantidad) {
        return new MoneyCliente(cantidad);
    }

    public static MoneyCliente of(String cantidad) {
        return new MoneyCliente(new BigDecimal(cantidad));
    }

    public static MoneyCliente of(long cantidad) {
        return new MoneyCliente(BigDecimal.valueOf(cantidad));
    }

    public MoneyCliente add(MoneyCliente otro) {
        return new MoneyCliente(Money.round(cantidad.add(otro.cantidad)).cantidad());
    }

    public MoneyCliente multiply(int veces) {
        return new MoneyCliente(Money.round(cantidad.multiply(BigDecimal.valueOf(veces))).cantidad());
    }

    public BigDecimal cuantos() {
        return cantidad;
    }

    @Override
    public int compareTo(MoneyCliente otro) {
        return cantidad.compareTo(otro.cantidad);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof MoneyCliente m)) {
            return false;
        }
        return cantidad.compareTo(m.cantidad) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(cantidad.stripTrailingZeros());
    }
}
