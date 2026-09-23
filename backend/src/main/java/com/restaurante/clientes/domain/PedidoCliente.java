package com.restaurante.clientes.domain;

import com.restaurante.shared.domain.BaseEntity;
import com.restaurante.shared.domain.exception.BusinessRuleException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Pedido del cliente. RF-40..45: BORRADOR -> CONFIRMADO -> EN_PREPARACION -> LISTO -> ENTREGADO.
 * Transiciones en UNA sola clase (misma regla que Pedido). Idempotencia RF-41: UNIQUE(cliente_id, idempotency_key).
 */
@Entity
@Table(name = "pedidos_clientes")
public class PedidoCliente extends BaseEntity {

    @Column(nullable = false, unique = true, length = 40)
    private String codigo;

    @Column(name = "cliente_id", nullable = false)
    private Long clienteId;

    @Column(name = "metodo_pago", nullable = false, length = 20)
    private String metodoPago;

    @Column(name = "metodo_entrega", nullable = false, length = 20)
    private String metodoEntrega;

    @Column(name = "direccion_id")
    private Long direccionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoPedidoCliente estado = EstadoPedidoCliente.BORRADOR;

    @Column(name = "idempotency_key", nullable = false, length = 100)
    private String idempotencyKey;

    @Version
    @Column(nullable = false)
    private Long version = 0L;

    @Column(name = "confirmado_at")
    private Instant confirmadoAt;

    @Column(name = "creado_at", nullable = false, updatable = false)
    private Instant creadoAt;

    @Column(name = "actualizado_at", nullable = false)
    private Instant actualizadoAt;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("id")
    private Set<PedidoClienteLinea> lineas = new LinkedHashSet<>();

    protected PedidoCliente() {
    }

    public PedidoCliente(String codigo, Long clienteId, String metodoPago, String metodoEntrega,
                         Long direccionId, String idempotencyKey) {
        this.codigo = codigo;
        this.clienteId = clienteId;
        this.metodoPago = metodoPago;
        this.metodoEntrega = metodoEntrega;
        this.direccionId = direccionId;
        this.idempotencyKey = idempotencyKey;
        this.estado = EstadoPedidoCliente.BORRADOR;
    }

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (creadoAt == null) creadoAt = now;
        if (actualizadoAt == null) actualizadoAt = now;
    }

    @PreUpdate
    void preUpdate() {
        actualizadoAt = Instant.now();
    }

    // ---- transiciones en una sola clase ----

    public void confirmar(String claveReintento) {
        if (estado != EstadoPedidoCliente.BORRADOR) {
            if (!idempotencyKey.equals(claveReintento)) {
                throw new BusinessRuleException("409 RF-41: clave idempotencia distinta (RF-24/25)");
            }
            return; // idempotente 200
        }
        if (lineas.isEmpty()) {
            throw new BusinessRuleException("RF-41: carrito vacío, no se confirma");
        }
        this.estado = EstadoPedidoCliente.CONFIRMADO;
        this.confirmadoAt = Instant.now();
    }

    public void marcarEnPreparacion() {
        if (estado != EstadoPedidoCliente.CONFIRMADO) {
            throw new BusinessRuleException("Solo CONFIRMADO -> EN_PREPARACION (actual=" + estado + ")");
        }
        this.estado = EstadoPedidoCliente.EN_PREPARACION;
    }

    public void marcarListo() {
        if (estado != EstadoPedidoCliente.EN_PREPARACION) {
            throw new BusinessRuleException("Solo EN_PREPARACION -> LISTO (actual=" + estado + ")");
        }
        this.estado = EstadoPedidoCliente.LISTO;
    }

    public void entregar() {
        if (estado != EstadoPedidoCliente.LISTO) {
            throw new BusinessRuleException("Solo LISTO -> ENTREGADO (actual=" + estado + ")");
        }
        this.estado = EstadoPedidoCliente.ENTREGADO;
    }

    public void anular() {
        if (estado != EstadoPedidoCliente.BORRADOR) {
            throw new BusinessRuleException("Solo BORRADOR se puede anular");
        }
        this.estado = EstadoPedidoCliente.ANULADO;
    }

    public void agregarLinea(PedidoClienteLinea linea) {
        if (estado != EstadoPedidoCliente.BORRADOR) {
            throw new BusinessRuleException("No se puede agregar línea a pedido " + estado);
        }
        linea.setPedido(this);
        lineas.add(linea);
    }

    // getters
    public String getCodigo() { return codigo; }
    public Long getClienteId() { return clienteId; }
    public String getMetodoPago() { return metodoPago; }
    public String getMetodoEntrega() { return metodoEntrega; }
    public Long getDireccionId() { return direccionId; }
    public EstadoPedidoCliente getEstado() { return estado; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public Long getVersion() { return version; }
    public Instant getConfirmadoAt() { return confirmadoAt; }
    public Instant getCreadoAt() { return creadoAt; }
    public Instant getActualizadoAt() { return actualizadoAt; }
    public Set<PedidoClienteLinea> getLineas() { return Set.copyOf(lineas); }
}
