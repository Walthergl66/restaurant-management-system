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
 * Recorrido de un repartidor: ruta INDIVIDUAL (un pedido) o MULTIPLE (varias
 * paradas cuyo orden lo optimiza ORS en fases posteriores). Guarda el criterio
 * de optimización y los totales calculados. Sus estados son logísticos y no
 * tocan el estado de preparación de los pedidos.
 */
@Entity
@Table(name = "recorridos", schema = "delivery")
public class Recorrido extends AuditableEntity {

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(name = "repartidor_id", nullable = false)
    private Long repartidorId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoRecorrido tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoRecorrido estado = EstadoRecorrido.PLANIFICADO;

    @Enumerated(EnumType.STRING)
    @Column(name = "criterio_optimizacion", nullable = false, length = 20)
    private CriterioOptimizacion criterioOptimizacion = CriterioOptimizacion.DURACION;

    @Column(name = "distancia_total_m")
    private Integer distanciaTotalM;

    @Column(name = "duracion_total_s")
    private Integer duracionTotalS;

    @Column(name = "iniciado_at")
    private Instant iniciadoAt;

    @Column(name = "finalizado_at")
    private Instant finalizadoAt;

    protected Recorrido() {
    }

    public Recorrido(Long repartidorId, TipoRecorrido tipo, CriterioOptimizacion criterioOptimizacion) {
        if (repartidorId == null) {
            throw new BusinessRuleException("El recorrido debe tener un repartidor");
        }
        if (tipo == null) {
            throw new BusinessRuleException("El recorrido debe tener un tipo");
        }
        this.repartidorId = repartidorId;
        this.tipo = tipo;
        if (criterioOptimizacion != null) {
            this.criterioOptimizacion = criterioOptimizacion;
        }
    }

    /**
     * Inicia el recorrido planificado y marca el momento de salida.
     */
    public void iniciar() {
        if (estado != EstadoRecorrido.PLANIFICADO) {
            throw new BusinessRuleException(
                    "Solo un recorrido planificado puede iniciarse");
        }
        this.estado = EstadoRecorrido.EN_CURSO;
        this.iniciadoAt = Instant.now();
    }

    /**
     * Finaliza el recorrido en curso cuando todas las paradas están resueltas.
     */
    public void finalizar() {
        if (estado != EstadoRecorrido.EN_CURSO) {
            throw new BusinessRuleException(
                    "Solo un recorrido en curso puede finalizarse");
        }
        this.estado = EstadoRecorrido.FINALIZADO;
        this.finalizadoAt = Instant.now();
    }

    public void registrarTotales(Integer distanciaTotalM, Integer duracionTotalS) {
        this.distanciaTotalM = distanciaTotalM;
        this.duracionTotalS = duracionTotalS;
    }

    public Long getRepartidorId() {
        return repartidorId;
    }

    public TipoRecorrido getTipo() {
        return tipo;
    }

    public EstadoRecorrido getEstado() {
        return estado;
    }

    public CriterioOptimizacion getCriterioOptimizacion() {
        return criterioOptimizacion;
    }

    public Integer getDistanciaTotalM() {
        return distanciaTotalM;
    }

    public Integer getDuracionTotalS() {
        return duracionTotalS;
    }

    public Instant getIniciadoAt() {
        return iniciadoAt;
    }

    public Instant getFinalizadoAt() {
        return finalizadoAt;
    }
}
