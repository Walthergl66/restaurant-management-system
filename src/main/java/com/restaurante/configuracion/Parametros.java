package com.restaurante.configuracion;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * API pública del módulo de configuración. Vive en el paquete raíz para que
 * otros módulos (catálogo, pedidos, facturación...) la consuman sin romper el
 * encapsulamiento que exige Spring Modulith.
 */
public interface Parametros {

    Optional<String> obtener(String clave);

    /**
     * IVA configurable en tanto por uno (0.15 = 15%), incluido en los precios
     * de catálogo. Usa el valor de la tabla si existe, si no el de la
     * configuración de aplicación.
     */
    BigDecimal tasaIva();

    String nombreRestaurante();

    String moneda();
}