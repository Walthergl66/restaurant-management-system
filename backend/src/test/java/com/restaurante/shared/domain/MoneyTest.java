package com.restaurante.shared.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class MoneyTest {

    @Test
    void redondeaSiempreA2Decimales() {
        assertThat(Money.of("10.005").getAmount()).isEqualByComparingTo("10.01");
        assertThat(Money.of("1.002").getAmount()).isEqualByComparingTo("1.00");
    }

    @Test
    void sumaRestayPorcentaje() {
        Money a = Money.of("10.50");
        Money b = Money.of("3.25");
        assertThat(a.add(b).getAmount()).isEqualByComparingTo(new BigDecimal("13.75"));
        assertThat(a.subtract(b).getAmount()).isEqualByComparingTo(new BigDecimal("7.25"));
        assertThat(a.percentageOf(new BigDecimal("0.15")).getAmount()).isEqualByComparingTo(new BigDecimal("1.58"));
    }

    @Test
    void multiplicaPorCantidadRedondeando() {
        Money unitario = Money.of("2.33");
        assertThat(unitario.multiply(new BigDecimal("3")).getAmount()).isEqualByComparingTo(new BigDecimal("6.99"));
    }

    @Test
    void comparaMontos() {
        assertThat(Money.of("5.00")).isEqualByComparingTo(Money.of("5.000"));
        assertThat(Money.of("1.00").add(Money.of("1")).isGreaterThan(Money.of("1.99"))).isTrue();
        assertThat(Money.ZERO.isZero()).isTrue();
    }

    @Test
    void valorIgualidadIgualesSegunRegla() {
        Money m1 = Money.of("2.123");
        Money m2 = Money.of("2.12");
        assertThat(m1).isEqualTo(m2);
        assertThat(m1.hashCode()).isEqualTo(m2.hashCode());
    }
}