package com.restaurante.delivery.domain;

import com.restaurante.shared.domain.AuditableEntity;
import com.restaurante.shared.domain.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;

/**
 * Parada ordenada dentro de un recorrido. El orden es 0-based (la primera es 0)
 * y lo decide ORS Optimization en fases posteriores. Cada parada resuelve la
 * asignación (pedido) a la que apunta; marcar entregada/fallida exige que esté
 * PENDIENTE para evitar la doble confirmación.
 */
@Entity
@Table(name = "paradas", schema = "delivery")
public class Parada extends AuditableEntity {

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(name = "recorrido_id", nullable = false)
    private Long recorridoId;

    @Column(name = "asignacion_id", nullable = false)
    private Long asignacionId;

    @Column(nullable = false)
    private Integer orden;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoParada estado = EstadoParada.PENDIENTE;

    @Column(name = "entregado_at")
    private Instant entregadoAt;

    // ---- Doble confirmación de entrega (conceptos SEPARADOS del estado logístico) ----

    /** Confirmación 1 de 2: cuándo el repartidor confirmó la entrega. */
    @Column(name = "repartidor_confirmo_at")
    private Instant repartidorConfirmoAt;

    /** Usuario repartidor que confirmó (auditoría de la confirmación del repartidor). */
    @Column(name = "repartidor_confirmo_por", length = 50)
    private String repartidorConfirmoPor;

    /** Confirmación 2 de 2: cuándo el cliente confirmó la recepción. */
    @Column(name = "cliente_confirmo_at")
    private Instant clienteConfirmoAt;

    /** Estado FINAL verificado: true solo con ambas confirmaciones (o resolución admin). */
    @Column(name = "entrega_verificada", nullable = false)
    private boolean entregaVerificada = false;

    /** Admin que resolvió una confirmación pendiente del cliente (incidencia). */
    @Column(name = "resuelta_por_admin", length = 50)
    private String resueltaPorAdmin;

    /** Motivo justificado de la resolución administrativa. */
    @Column(name = "resuelta_motivo", length = 300)
    private String resueltaMotivo;

    protected Parada() {
    }

    public Parada(Long recorridoId, Long asignacionId, Integer orden) {
        if (recorridoId == null) {
            throw new BusinessRuleException("La parada debe pertenecer a un recorrido");
        }
        if (asignacionId == null) {
            throw new BusinessRuleException("La parada debe referir a una asignación");
        }
        if (orden == null || orden < 0) {
            throw new BusinessRuleException("El orden de la parada no puede ser negativo");
        }
        this.recorridoId = recorridoId;
        this.asignacionId = asignacionId;
        this.orden = orden;
    }

    /**
     * Marca la parada como entregada: solo si está PENDIENTE (evita doble
     * confirmación).
     */
    public void marcarEntregada() {
        requierePendiente();
        this.estado = EstadoParada.ENTREGADA;
        this.entregadoAt = Instant.now();
    }

    /**
     * Marca la parada como fallida: solo si está PENDIENTE.
     */
    public void marcarFallida() {
        requierePendiente();
        this.estado = EstadoParada.FALLIDA;
    }

    private void requierePendiente() {
        if (estado != EstadoParada.PENDIENTE) {
            throw new BusinessRuleException(
                    "Una parada " + estado.name().toLowerCase() + " no admite otra confirmación");
        }
    }

    // ---- Doble confirmación ----

    /**
     * Confirmación del REPARTIDOR (1 de 2): el repartidor llegó/entregó. Valida
     * que la parada esté PENDIENTE (reusa {@link #marcarEntregada()}: pasa a
     * ENTREGADA en lo LOGÍSTICO) y registra la confirmación del repartidor.
     * Idempotencia: el reintento con una parada ya ENTREGADA lo gestiona el
     * servicio (devuelve sin re-aplicar); aquí una parada FALLIDA se rechaza.
     */
    public void confirmarPorRepartidor(String usuario) {
        marcarEntregada();
        this.repartidorConfirmoAt = this.entregadoAt;
        this.repartidorConfirmoPor = usuario;
    }

    /**
     * Confirmación del CLIENTE (2 de 2): el cliente recibió el pedido. Exige que
     * el repartidor ya haya confirmado (el cliente no puede confirmar antes). Si
     * con esta confirmación existen ambas, marca la entrega VERIFICADA.
     * Idempotente: si el cliente ya confirmó, no duplica (no re-escribe la fecha).
     */
    public void confirmarPorCliente() {
        if (repartidorConfirmoAt == null) {
            throw new BusinessRuleException(
                    "El pedido aún no fue entregado por el repartidor: no se puede confirmar la recepción");
        }
        if (clienteConfirmoAt == null) {
            this.clienteConfirmoAt = Instant.now();
        }
        this.entregaVerificada = true;
    }

    /**
     * Resolución ADMINISTRATIVA y AUDITADA de una entrega cuyo cliente nunca
     * confirmó: exige que el repartidor haya confirmado y un motivo. Marca la
     * entrega VERIFICADA registrando quién la resolvió y por qué. No hay cierre
     * automático por tiempo: solo esta acción humana justificada.
     */
    public void resolverPorAdmin(String admin, String motivo) {
        if (repartidorConfirmoAt == null) {
            throw new BusinessRuleException(
                    "No hay confirmación del repartidor que resolver para esta parada");
        }
        if (entregaVerificada) {
            throw new BusinessRuleException("La entrega ya está verificada");
        }
        if (motivo == null || motivo.isBlank()) {
            throw new BusinessRuleException("La resolución administrativa requiere un motivo");
        }
        this.resueltaPorAdmin = admin;
        this.resueltaMotivo = motivo;
        this.entregaVerificada = true;
    }

    /** Fase legible de la doble confirmación (concepto separado del estado logístico). */
    public FaseConfirmacion faseConfirmacion() {
        if (estado == EstadoParada.FALLIDA) {
            return FaseConfirmacion.FALLIDA;
        }
        if (entregaVerificada) {
            return FaseConfirmacion.VERIFICADA;
        }
        if (repartidorConfirmoAt != null) {
            return FaseConfirmacion.PENDIENTE_CONFIRMACION_CLIENTE;
        }
        return FaseConfirmacion.SIN_CONFIRMAR;
    }

    public Long getRecorridoId() {
        return recorridoId;
    }

    public Long getAsignacionId() {
        return asignacionId;
    }

    public Integer getOrden() {
        return orden;
    }

    public EstadoParada getEstado() {
        return estado;
    }

    public Instant getEntregadoAt() {
        return entregadoAt;
    }

    public Instant getRepartidorConfirmoAt() {
        return repartidorConfirmoAt;
    }

    public String getRepartidorConfirmoPor() {
        return repartidorConfirmoPor;
    }

    public Instant getClienteConfirmoAt() {
        return clienteConfirmoAt;
    }

    public boolean isEntregaVerificada() {
        return entregaVerificada;
    }

    public String getResueltaPorAdmin() {
        return resueltaPorAdmin;
    }

    public String getResueltaMotivo() {
        return resueltaMotivo;
    }
}
