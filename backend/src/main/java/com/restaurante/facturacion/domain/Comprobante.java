package com.restaurante.facturacion.domain;

import com.restaurante.shared.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Comprobante emitido con numeración secuencial. Es solo-lectura después de la
 * emisión: no hay edición de comprobantes.
 */
@Entity
@Table(name = "comprobantes")
public class Comprobante extends AuditableEntity {

    @Column(nullable = false, unique = true, length = 20)
    private String correlativo;

    @Column(nullable = false, unique = true)
    private Long secuencial;

    @Column(name = "tipo", nullable = false, length = 10)
    private String tipo;

    @Column(name = "cuenta_id", nullable = false)
    private Long cuentaId;

    @Column(nullable = false)
    private BigDecimal total;

    @Column(name = "cliente_nombre", length = 120)
    private String clienteNombre;

    @Column(name = "cliente_identificacion", length = 20)
    private String clienteIdentificacion;

    @Column(name = "fecha", nullable = false)
    private Instant fecha;

    @Column(name = "emitido_por", length = 50)
    private String emitidoPor;

    protected Comprobante() {
    }

    public Comprobante(String correlativo, Long secuencial, String tipo, Long cuentaId,
                       BigDecimal total, String clienteNombre, String clienteIdentificacion,
                       String emitidoPor) {
        this.correlativo = correlativo;
        this.secuencial = secuencial;
        this.tipo = tipo;
        this.cuentaId = cuentaId;
        this.total = total;
        this.clienteNombre = clienteNombre;
        this.clienteIdentificacion = clienteIdentificacion;
        this.fecha = Instant.now();
        this.emitidoPor = emitidoPor;
    }

    public String getCorrelativo() {
        return correlativo;
    }

    public Long getSecuencial() {
        return secuencial;
    }

    public String getTipo() {
        return tipo;
    }

    public Long getCuentaId() {
        return cuentaId;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public String getClienteNombre() {
        return clienteNombre;
    }

    public String getClienteIdentificacion() {
        return clienteIdentificacion;
    }

    public Instant getFecha() {
        return fecha;
    }

    public String getEmitidoPor() {
        return emitidoPor;
    }
}