package com.restaurante.comandas.domain;

import com.restaurante.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Línea de una comanda, con los datos congelados al confirmar el pedido.
 */
@Entity
@Table(name = "comanda_lineas")
public class ComandaLinea extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "comanda_id", nullable = false)
    private Comanda comanda;

    @Column(name = "producto_id", nullable = false)
    private Long productoId;

    @Column(name = "nombre_producto", nullable = false, length = 120)
    private String nombreProducto;

    @Column(nullable = false)
    private int cantidad;

    @Column(length = 300)
    private String extras;

    @Column(length = 300)
    private String ingredientes;

    @Column(length = 500)
    private String observaciones;

    @Column(nullable = false)
    private int orden;

    protected ComandaLinea() {
    }

    public ComandaLinea(Long productoId, String nombreProducto, int cantidad,
                        String extras, String ingredientes, String observaciones, int orden) {
        this.productoId = productoId;
        this.nombreProducto = nombreProducto;
        this.cantidad = cantidad;
        this.extras = extras;
        this.ingredientes = ingredientes;
        this.observaciones = observaciones;
        this.orden = orden;
    }

    void agregarAComanda(Comanda comanda) {
        this.comanda = comanda;
    }

    public Long getProductoId() {
        return productoId;
    }

    public String getNombreProducto() {
        return nombreProducto;
    }

    public int getCantidad() {
        return cantidad;
    }

    public String getExtras() {
        return extras;
    }

    public String getIngredientes() {
        return ingredientes;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public int getOrden() {
        return orden;
    }
}