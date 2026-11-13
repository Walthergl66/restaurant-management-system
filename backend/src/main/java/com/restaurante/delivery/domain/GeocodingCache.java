package com.restaurante.delivery.domain;

import com.restaurante.shared.domain.BaseEntity;
import com.restaurante.shared.domain.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Coordenadas cacheadas de una dirección para no regeocodificar y no gastar la
 * cuota de ORS. Única por direccion_id. Guarda el texto de la dirección para
 * invalidar la caché si la dirección original cambia.
 */
@Entity
@Table(name = "geocoding_cache", schema = "delivery")
public class GeocodingCache extends BaseEntity {

    @Column(name = "direccion_id", nullable = false, unique = true)
    private Long direccionId;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal lat;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal lng;

    @Column(name = "direccion_texto", length = 200)
    private String direccionTexto;

    @Column(name = "geocodificado_at", nullable = false)
    private Instant geocodificadoAt = Instant.now();

    protected GeocodingCache() {
    }

    public GeocodingCache(Long direccionId, BigDecimal lat, BigDecimal lng, String direccionTexto) {
        if (direccionId == null) {
            throw new BusinessRuleException("La caché de geocoding debe referir a una dirección");
        }
        if (lat == null || lng == null) {
            throw new BusinessRuleException("La caché de geocoding necesita latitud y longitud");
        }
        this.direccionId = direccionId;
        this.lat = lat;
        this.lng = lng;
        this.direccionTexto = direccionTexto;
        this.geocodificadoAt = Instant.now();
    }

    public Long getDireccionId() {
        return direccionId;
    }

    public BigDecimal getLat() {
        return lat;
    }

    public BigDecimal getLng() {
        return lng;
    }

    public String getDireccionTexto() {
        return direccionTexto;
    }

    public Instant getGeocodificadoAt() {
        return geocodificadoAt;
    }
}
