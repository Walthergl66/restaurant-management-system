package com.restaurante.clientes.domain;

import com.restaurante.shared.domain.BaseEntity;
import com.restaurante.shared.domain.Money;
import com.restaurante.shared.infrastructure.MoneyConverter;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "pedidos_clientes_lineas")
public class PedidoClienteLinea extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pedido_id", nullable = false)
    private PedidoCliente pedido;

    @Column(name = "producto_id", nullable = false)
    private Long productoId;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Convert(converter = MoneyConverter.class)
    @Column(nullable = false)
    private Money precio;

    @Column(nullable = false)
    private Integer cantidad;

    @Column(length = 200)
    private String observaciones;

    @Column(name = "creado_at", nullable = false, updatable = false)
    private Instant creadoAt;

    @OneToMany(mappedBy = "linea", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private Set<PedidoClienteLineaExtra> extras = new LinkedHashSet<>();

    protected PedidoClienteLinea() {
    }

    public PedidoClienteLinea(Long productoId, String nombre, Money precio, int cantidad, String observaciones) {
        if (cantidad <= 0) {
            throw new com.restaurante.shared.domain.exception.BusinessRuleException("La cantidad debe ser mayor que cero");
        }
        this.productoId = productoId;
        this.nombre = nombre;
        this.precio = precio;
        this.cantidad = cantidad;
        this.observaciones = observaciones;
    }

    @PrePersist
    void prePersist() {
        if (creadoAt == null) creadoAt = Instant.now();
    }

    public Money subtotal() {
        Money base = precio.multiply(java.math.BigDecimal.valueOf(cantidad));
        Money extraTotal = extras.stream()
                .map(e -> e.getPrecio().multiply(java.math.BigDecimal.valueOf(cantidad)))
                .reduce(Money.ZERO, Money::add);
        return base.add(extraTotal);
    }

    public void setPedido(PedidoCliente pedido) { this.pedido = pedido; }
    public PedidoCliente getPedido() { return pedido; }
    public Long getProductoId() { return productoId; }
    public String getNombre() { return nombre; }
    public Money getPrecio() { return precio; }
    public Integer getCantidad() { return cantidad; }
    public String getObservaciones() { return observaciones; }
    public Instant getCreadoAt() { return creadoAt; }
    public Set<PedidoClienteLineaExtra> getExtras() { return Set.copyOf(extras); }

    public void agregarExtra(PedidoClienteLineaExtra extra) {
        extra.setLinea(this);
        extras.add(extra);
    }
}
