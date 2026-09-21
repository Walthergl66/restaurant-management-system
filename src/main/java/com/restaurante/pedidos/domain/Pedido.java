package com.restaurante.pedidos.domain;

import com.restaurante.shared.domain.AuditableEntity;
import com.restaurante.shared.domain.Money;
import com.restaurante.shared.domain.exception.BusinessRuleException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Pedido. Agregado que concentra, en una sola clase, TODAS las transiciones de
 * estado del ciclo de vida (regla de negocio 1) y la congelación de precios.
 *
 * <p>Al confirmar queda BLOQUEADO: ya no admite agregar, editar ni quitar
 * líneas (RF-15/RF-16). El código lo genera la app para poder reintentar sin
 * duplicar (RNF-06); la unicidad la valida la base de datos.
 */
@Entity
@Table(name = "pedidos")
public class Pedido extends AuditableEntity {

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(nullable = false, unique = true, length = 40)
    private String codigo;

    @Column(name = "mesa_id")
    private Long mesaId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoPedido estado = EstadoPedido.BORRADOR;

    @Column(length = 500)
    private String notas;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private Set<PedidoLinea> lineas = new LinkedHashSet<>();

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Confirmacion> confirmaciones = new LinkedHashSet<>();

    protected Pedido() {
    }

    public Pedido(String codigo, Long mesaId, String notas) {
        this.codigo = validarCodigo(codigo);
        if (mesaId == null) {
            throw new BusinessRuleException("El pedido debe ligarse a una mesa");
        }
        this.mesaId = mesaId;
        this.notas = notas;
    }

    public PedidoLinea agregarLinea(
            Long productoId,
            String nombreProducto,
            Money precioUnitario,
            int cantidad,
            Set<ExtraLinea> extras,
            Set<IngredienteRemovido> ingredientes,
            String observaciones) {
        requiereBorrador();
        PedidoLinea linea = new PedidoLinea(productoId, nombreProducto, precioUnitario, cantidad);
        linea.agregarAlPedido(this);
        linea.actualizar(cantidad, extras, ingredientes, observaciones);
        lineas.add(linea);
        return linea;
    }

    public void actualizarLinea(
            Long lineaId,
            int cantidad,
            Set<ExtraLinea> extras,
            Set<IngredienteRemovido> ingredientes,
            String observaciones) {
        requiereBorrador();
        PedidoLinea linea = buscarLinea(lineaId);
        linea.actualizar(cantidad, extras, ingredientes, observaciones);
    }

    public void quitarLinea(Long lineaId) {
        requiereBorrador();
        PedidoLinea linea = buscarLinea(lineaId);
        lineas.remove(linea);
    }

    /**
     * Revalida precio y disponibilidad contra el catálogo y congela la línea
     * (RF-09). Deja el pedido CONFIRMADO; solo tras esto la cuenta puede actuar.
     */
    public void confirmar() {
        requiereBorrador();
        if (lineas.isEmpty()) {
            throw new BusinessRuleException("No se puede confirmar un pedido sin líneas");
        }
        this.estado = EstadoPedido.CONFIRMADO;
    }

    public void marcarEnPreparacion() {
        if (estado != EstadoPedido.CONFIRMADO) {
            throw new BusinessRuleException("Solo un pedido confirmado puede pasar a preparación (estado actual: " + estado + ")");
        }
        this.estado = EstadoPedido.EN_PREPARACION;
    }

    public void marcarListo() {
        if (estado != EstadoPedido.EN_PREPARACION) {
            throw new BusinessRuleException("Solo un pedido en preparación puede marcarse como listo (estado actual: " + estado + ")");
        }
        this.estado = EstadoPedido.LISTO;
    }

    public void entregar() {
        if (estado != EstadoPedido.LISTO) {
            throw new BusinessRuleException("Solo un pedido listo puede entregarse (estado actual: " + estado + ")");
        }
        this.estado = EstadoPedido.ENTREGADO;
    }

    public Money totalLineas() {
        return lineas.stream()
                .map(PedidoLinea::subtotal)
                .reduce(Money.ZERO, Money::add);
    }

    public void agregarConfirmacion(String idempotencyKey) {
        Confirmacion confirmacion = new Confirmacion(this, idempotencyKey);
        confirmaciones.add(confirmacion);
    }

    public boolean tieneConfirmacionConClave(String idempotencyKey) {
        return confirmaciones.stream().anyMatch(c -> c.tieneLaMismaClave(idempotencyKey));
    }

    private void requiereBorrador() {
        if (estado != EstadoPedido.BORRADOR) {
            throw new BusinessRuleException(
                    "Un pedido " + estado.name().toLowerCase() + " está bloqueado y no admite cambios");
        }
    }

    private PedidoLinea buscarLinea(Long lineaId) {
        return lineas.stream()
                .filter(l -> l.getId().equals(lineaId))
                .findFirst()
                .orElseThrow(() -> new BusinessRuleException("La línea indicada no pertenece al pedido"));
    }

    private String validarCodigo(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            throw new BusinessRuleException("El código del pedido es obligatorio");
        }
        if (codigo.length() > 40) {
            throw new BusinessRuleException("El código del pedido no puede superar 40 caracteres");
        }
        return codigo;
    }

    public String getCodigo() {
        return codigo;
    }

    public Long getMesaId() {
        return mesaId;
    }

    public EstadoPedido getEstado() {
        return estado;
    }

    public Optional<String> getNotas() {
        return Optional.ofNullable(notas);
    }

    public List<PedidoLinea> getLineas() {
        return List.copyOf(lineas);
    }

    public List<Confirmacion> getConfirmaciones() {
        return List.copyOf(confirmaciones);
    }
}