package com.restaurante.clientes.infrastructure;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

/**
 * Parámetros del canal de domicilio del cliente. Hoy solo el recargo fijo que
 * se suma al total de un pedido DOMICILIO; se expone como propiedad para poder
 * ajustarlo sin tocar código (bloque {@code app.delivery} de application.yml).
 */
@ConfigurationProperties(prefix = "app.delivery")
public class ClientesDeliveryProperties {

    /** Recargo fijo por pedido a domicilio. Default 1.50. */
    private BigDecimal recargoDomicilio = new BigDecimal("1.50");

    public BigDecimal getRecargoDomicilio() {
        return recargoDomicilio;
    }

    public void setRecargoDomicilio(BigDecimal recargoDomicilio) {
        this.recargoDomicilio = recargoDomicilio;
    }
}
