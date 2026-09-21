package com.restaurante.anulaciones.domain;

import com.restaurante.shared.domain.AuditableEntity;
import com.restaurante.shared.domain.Money;
import com.restaurante.shared.domain.exception.BusinessRuleException;
import com.restaurante.shared.infrastructure.MoneyConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;

/**
 * Anulación de una línea de pedido. Registro aparte con congelado de nombre,
 * precio y cantidad al solicitarse (RF-20 a RF-23). Sus transiciones de estado
 * viven únicamente aquí; la línea original nunca se toca.
 */
@Entity
@Table(name = "anulaciones")
public class Anulacion extends AuditableEntity {

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(name = "pedido_codigo", nullable = false, length = 40)
    private String pedidoCodigo;

    @Column(name = "linea_id", nullable = false)
    private Long lineaId;

    @Column(name = "producto_id", nullable = false)
    private Long productoId;

    @Column(name = "nombre_producto", nullable = false, length = 120)
    private String nombreProducto;

    @Convert(converter = MoneyConverter.class)
    @Column(name = "precio_unitario", nullable = false)
    private Money precioUnitario;

    @Column(nullable = false)
    private int cantidad;

    @Column(length = 300)
    private String motivo;

    @Column(name = "area_id")
    private Long areaId;

    @Column(name = "area_nombre", length = 80)
    private String areaNombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoAnulacion estado = EstadoAnulacion.SOLICITADA;

    @Column(name = "solicitado_por", length = 50)
    private String solicitadoPor;

    @Column(name = "resuelto_por", length = 50)
    private String resueltoPor;

    @Column(name = "resuelta_at")
    private Instant resueltaAt;

    protected Anulacion() {
    }

    public Anulacion(String pedidoCodigo, Long lineaId, Long productoId, String nombreProducto,
                     Money precioUnitario, int cantidad, String motivo,
                     Long areaId, String areaNombre, String solicitadoPor) {
        this.pedidoCodigo = pedidoCodigo;
        this.lineaId = lineaId;
        this.productoId = productoId;
        this.nombreProducto = nombreProducto;
        this.precioUnitario = precioUnitario;
        this.cantidad = validarCantidad(cantidad);
        this.motivo = motivo;
        this.areaId = areaId;
        this.areaNombre = areaNombre;
        this.solicitadoPor = solicitadoPor;
    }

    /**
     * Solo una anulación SOLICITADA puede aprobarse.
     */
    public void aprobar(String resueltoPor) {
        requiereSolicitada();
        this.estado = EstadoAnulacion.APROBADA;
        resolver(resueltoPor);
    }

    public void rechazar(String resueltoPor) {
        requiereSolicitada();
        this.estado = EstadoAnulacion.RECHAZADA;
        resolver(resueltoPor);
    }

    private void requiereSolicitada() {
        if (estado != EstadoAnulacion.SOLICITADA) {
            throw new BusinessRuleException(
                    "Una anulación " + estado.name().toLowerCase() + " no admite resolución");
        }
    }

    private void resolver(String usuario) {
        this.resueltoPor = usuario;
        this.resueltaAt = Instant.now();
    }

    private int validarCantidad(int cantidad) {
        if (cantidad <= 0) {
            throw new BusinessRuleException("La cantidad a anular debe ser mayor que cero");
        }
        return cantidad;
    }

    public String getPedidoCodigo() {
        return pedidoCodigo;
    }

    public Long getLineaId() {
        return lineaId;
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

    public String getMotivo() {
        return motivo;
    }

    public Long getAreaId() {
        return areaId;
    }

    public String getAreaNombre() {
        return areaNombre;
    }

    public EstadoAnulacion getEstado() {
        return estado;
    }

    public String getSolicitadoPor() {
        return solicitadoPor;
    }

    public String getResueltoPor() {
        return resueltoPor;
    }

    public Instant getResueltaAt() {
        return resueltaAt;
    }
}