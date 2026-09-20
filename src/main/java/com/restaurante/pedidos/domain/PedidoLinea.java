package com.restaurante.pedidos.domain;

import com.restaurante.shared.domain.AuditableEntity;
import com.restaurante.shared.domain.Money;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Línea de pedido. Congela el nombre y precio del producto y sus extras en el
 * momento en que se confirma el pedido (RF-09, regla de precios congelados).
 */
public class PedidoLinea extends AuditableEntity {

    private Pedido pedido;

    private Long productoId;

    private String nombreProducto;

    private Money precioUnitario;

    private int cantidad;

    private String observaciones;

    private List<ExtraLinea> extras = new ArrayList<>();

    private List<IngredienteRemovido> ingredientesRemovidos = new ArrayList<>();

    protected PedidoLinea() {
    }

    public PedidoLinea(Long productoId, String nombreProducto, Money precioUnitario, int cantidad) {
        this.productoId = productoId;
        this.nombreProducto = nombreProducto;
        this.precioUnitario = precioUnitario;
        this.cantidad = validarCantidad(cantidad);
    }

    void agregarAlPedido(Pedido pedido) {
        this.pedido = pedido;
    }

    /**
     * Congela (o recongela) nombre, precio unitario y extras desde el catálogo.
     * Se aplica al confirmar: si algún ítem cambió de precio respecto del
     * borrador, la línea se recalcula con el precio vigente (RF-09).
     */
    public void congelarCatalogo(
            String nombreProducto,
            Money precioUnitario,
            List<ExtraLinea> extras) {
        this.nombreProducto = nombreProducto;
        this.precioUnitario = precioUnitario;
        this.extras.clear();
        if (extras != null) {
            this.extras = new ArrayList<>(extras);
            this.extras.forEach(e -> e.agregarALinea(this));
        }
    }

    public void actualizar(int cantidad, List<ExtraLinea> extras, List<IngredienteRemovido> ingredientes, String observaciones) {
        this.cantidad = validarCantidad(cantidad);
        this.extras.clear();
        if (extras != null) {
            this.extras = new ArrayList<>(extras);
            this.extras.forEach(e -> e.agregarALinea(this));
        }
        this.ingredientesRemovidos.clear();
        if (ingredientes != null) {
            this.ingredientesRemovidos = new ArrayList<>(ingredientes);
            this.ingredientesRemovidos.forEach(i -> i.agregarALinea(this));
        }
        this.observaciones = observaciones;
    }

    public void cambiarObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    public boolean esDelProducto(Long productoId) {
        return this.productoId.equals(productoId);
    }

    public Money subtotal() {
        Money base = precioUnitario.multiply(java.math.BigDecimal.valueOf(cantidad));
        Money extrasTotal = extras.stream()
                .map(ExtraLinea::getPrecio)
                .reduce(Money.ZERO, Money::add)
                .multiply(java.math.BigDecimal.valueOf(cantidad));
        return base.add(extrasTotal);
    }

    private int validarCantidad(int cantidad) {
        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor a cero");
        }
        return cantidad;
    }

    public Pedido getPedido() {
        return pedido;
    }

    public Long getProductoId() {
        return productoId;
    }

    public String getNombreProducto() {
        return nombreProducto;
    }

    public Money getPrecioUnitario() {
        return precioUnitario;
    }

    public int getCantidad() {
        return cantidad;
    }

    public Optional<String> getObservaciones() {
        return Optional.ofNullable(observaciones);
    }

    public List<ExtraLinea> getExtras() {
        return List.copyOf(extras);
    }

    public List<IngredienteRemovido> getIngredientesRemovidos() {
        return List.copyOf(ingredientesRemovidos);
    }
}