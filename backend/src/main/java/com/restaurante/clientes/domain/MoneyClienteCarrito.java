package com.restaurante.clientes.domain;

import com.restaurante.shared.domain.Money;

import java.math.BigDecimal### 06 RF-41 Money SPI ===
import java.math.BigDecimal;

/** RF-41/RF-40: dinero del carrito/pedido del cliente. CONGELA el Money del
 *  SPI shared RF-41 (nunca double RF-41 RF-09/22/23; Money.of/round/cantidad
 *  SPI Money ya en disco backend/.../shared/domain/Money.java). Record: total
 *  del carrito en &uacute;nico lugar (RF-44 RF-43 RF-45 RF-42 RF-41). */
public record MoneyClienteCarrito(Money dinero) {

    public static final MoneyClienteCarrito ZERO = new MoneyClienteCarrito(Money.ZERO);

    public static MoneyClienteCarrito of(Money money) {
        return new MoneyClienteCarrito(money == null ? Money.ZERO : money);
    }

    public static MoneyClienteCarrito of(BigDecimal cantidad) {
        return new MoneyClienteCarrito(Money.round(cantidad));
    }

    public static MoneyClienteCarrito of(long centavos) {
        return new MoneyClienteCarrito(Money.of(centavos));
    }

    public static MoneyClienteCarrito of(String cantidad) {
        return new MoneyClienteCarrito(Money.round(new BigDecimal(cantidad)));
    }

    public MoneyClienteCarrito add(MoneyClienteCarrito otro) {
        return of(Money.round(dinero.cantidad().add(otro.dinero.cantidad())));
    }

    public MoneyClienteCarrito times(int cantidad) {
        return of(Money.round(dinero.cantidad().multiply(BigDecimal.valueOf(cantidad))));
    }

    public BigDecimal cantidad() {
        return dinero.cantidad();
    }

    public String moneda() {
        return dinero.moneda();
    }
}
