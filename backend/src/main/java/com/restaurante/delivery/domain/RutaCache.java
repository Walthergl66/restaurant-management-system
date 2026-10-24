package com.restaurante.delivery.domain;

import com.restaurante.shared.domain.BaseEntity;
import com.restaurante.shared.domain.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Ruta cacheada (geometría y resumen de ORS Directions) reutilizable mientras
 * siga válida. clave_hash identifica el conjunto origen+paradas ordenadas para
 * reusar el cálculo. recorrido_id es nullable para rutas puntuales. La
 * reutilización real se implementa en las fases ORS; aquí solo el esquema.
 */
@Entity
@Table(name = "rutas_cache", schema = "delivery")
public class RutaCache extends BaseEntity {

    @Column(name = "recorrido_id")
    private Long recorridoId;

    @Column(name = "clave_hash", nullable = false, length = 64)
    private String claveHash;

    @Column(columnDefinition = "TEXT")
    private String geometria;

    @Column(name = "distancia_m")
    private Integer distanciaM;

    @Column(name = "duracion_s")
    private Integer duracionS;

    @Column(name = "calculado_at", nullable = false)
    private Instant calculadoAt = Instant.now();

    @Column(name = "expira_at")
    private Instant expiraAt;

    protected RutaCache() {
    }

    public RutaCache(Long recorridoId, String claveHash, String geometria,
                     Integer distanciaM, Integer duracionS, Instant expiraAt) {
        if (claveHash == null || claveHash.isBlank()) {
            throw new BusinessRuleException("La caché de ruta necesita una clave de reuso");
        }
        this.recorridoId = recorridoId;
        this.claveHash = claveHash;
        this.geometria = geometria;
        this.distanciaM = distanciaM;
        this.duracionS = duracionS;
        this.calculadoAt = Instant.now();
        this.expiraAt = expiraAt;
    }

    public Long getRecorridoId() {
        return recorridoId;
    }

    public String getClaveHash() {
        return claveHash;
    }

    public String getGeometria() {
        return geometria;
    }

    public Integer getDistanciaM() {
        return distanciaM;
    }

    public Integer getDuracionS() {
        return duracionS;
    }

    public Instant getCalculadoAt() {
        return calculadoAt;
    }

    public Instant getExpiraAt() {
        return expiraAt;
    }
}
