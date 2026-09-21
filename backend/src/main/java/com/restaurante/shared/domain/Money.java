package com.restaurante.shared.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Dinero del sistema. Toda operación monetaria pasa por aquí; el redondeo y la
 * escala se definen en UN solo lugar (nunca usar {@code double} para dinero).
 *
 * <p>Escala 2 con {@link RoundingMode#HALF_UP}, el estándar para precios.
 */
public final class Money extends ValueObject implements Comparable<Money> {

    private static final int SCALE = 2;
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

    public static final Money ZERO = Money.of("0");

    private final BigDecimal amount;

    private Money(BigDecimal amount) {
        this.amount = amount.setScale(SCALE, ROUNDING_MODE);
    }

    public static Money of(BigDecimal amount) {
        if (amount == null) {
            throw new IllegalArgumentException("El monto no puede ser nulo");
        }
        return new Money(amount);
    }

    public static Money of(String amount) {
        return of(new BigDecimal(amount));
    }

    public static Money of(long amount) {
        return of(new BigDecimal(amount));
    }

    /**
     * Aplica la regla de redondeo del sistema a una escala arbitraria.
     */
    public static BigDecimal round(BigDecimal value) {
        return value.setScale(SCALE, ROUNDING_MODE);
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public Money add(Money other) {
        return new Money(amount.add(other.amount));
    }

    public Money subtract(Money other) {
        return new Money(amount.subtract(other.amount));
    }

    public Money negate() {
        return new Money(amount.negate());
    }

    public Money multiply(BigDecimal factor) {
        return new Money(amount.multiply(factor));
    }

    /**
     * Porcentaje sobre este monto ({@code 0.15} = 15%).
     */
    public Money percentageOf(BigDecimal percentage) {
        return new Money(amount.multiply(percentage));
    }

    public boolean isZero() {
        return amount.signum() == 0;
    }

    public boolean isNegative() {
        return amount.signum() < 0;
    }

    public boolean isGreaterThan(Money other) {
        return amount.compareTo(other.amount) > 0;
    }

    public boolean isLessThan(Money other) {
        return amount.compareTo(other.amount) < 0;
    }

    @Override
    public List<?> getValueObjects() {
        return List.of(amount);
    }

    @Override
    public int compareTo(Money o) {
        return amount.compareTo(o.amount);
    }

    @Override
    public String toString() {
        return amount.toPlainString();
    }
}