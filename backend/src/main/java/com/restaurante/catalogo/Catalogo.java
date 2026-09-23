package com.restaurante.catalogo;

import java.util.List;
import java.util.Optional;

/**
 * API pública del catálogo. Vive en el paquete raíz para que otros módulos
 * (pedidos, comandas, facturación...) consulten productos sin romper el
 * encapsulamiento que exige Spring Modulith.
 */
public interface Catalogo {

    /**
     * Información congelada de un producto para armar un pedido.
     */
    Optional<ProductoParaPedido> productoParaPedido(Long productoId);

    /**
     * Información de un extra para congelarlo en una línea de pedido.
     */
    Optional<ExtraParaPedido> extraParaPedido(Long extraId);

    /**
     * Productos activos para menú (RF-40). Lista congelable con Money.
     */
    List<ProductoParaPedido> productosActivos();
}