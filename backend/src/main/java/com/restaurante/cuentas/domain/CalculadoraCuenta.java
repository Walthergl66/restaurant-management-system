package com.restaurante.cuentas.domain;

import com.restaurante.anulaciones.AnulacionResumen;
import com.restaurante.pedidos.PedidoResumen;
import com.restaurante.shared.domain.Money;
import com.restaurante.shared.domain.exception.BusinessRuleException;

import java.math.BigDecimal;
import java.util.List;

/**
 * ÚNICA clase que calcula el total de una cuenta (RNF-16): pedidos confirmados
 * menos anulaciones aprobadas, todo leído de los registros congelados. No
 * importa el orden en que lleguen pedidos y anulaciones: el resultado es la
 * suma exacta.
 */
public final class CalculadoraCuenta {

    /**
     * @param pedidos   pedidos que ya cuentan (confirmados o superiores)
     * @param aprobadas anulaciones aprobadas (ya incluidas en registros aparte)
     */
    public Money total(List<PedidoResumen> pedidos, List<AnulacionResumen> aprobadas) {
        Money consumo = pedidos.stream()
                .flatMap(p -> p.lineas().stream())
                .map(l -> Money.of(l.subtotal()))
                .reduce(Money.ZERO, Money::add);

        Money descuento = aprobadas.stream()
                .map(a -> Money.of(a.precioUnitario()).multiply(BigDecimal.valueOf(a.cantidad())))
                .reduce(Money.ZERO, Money::add);

        Money total = consumo.subtract(descuento);
        if (total.isNegative()) {
            throw new BusinessRuleException(
                    "El total de la cuenta no puede ser negativo (consumo " + consumo + ", descuento " + descuento + ")");
        }
        return total;
    }
}