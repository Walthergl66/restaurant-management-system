package com.restaurante.shared.infrastructure;

import com.restaurante.shared.domain.Money;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.math.BigDecimal;

/**
 * Convierte {@link Money} a {@link BigDecimal} para persistencia JPA.
 * El {@link Money} aplica siempre la regla de redondeo única del sistema.
 */
@Converter
public class MoneyConverter implements AttributeConverter<Money, BigDecimal> {

    @Override
    public BigDecimal convertToDatabaseColumn(Money attribute) {
        return attribute == null ? null : attribute.getAmount();
    }

    @Override
    public Money convertToEntityAttribute(BigDecimal dbData) {
        return dbData == null ? null : Money.of(dbData);
    }
}