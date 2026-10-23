package com.restaurante.delivery.domain;

import com.restaurante.shared.domain.AuditableEntity;
import com.restaurante.shared.domain.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Configuración de la ubicación del restaurante: el ORIGEN de todas las rutas
 * de reparto. Registro único (fila única en la tabla) que el administrador edita
 * desde el panel.
 *
 * <p>La dirección es obligatoria. Las coordenadas pueden faltar: una dirección
 * recién introducida que ORS no logra geocodificar queda PENDIENTE de selección
 * manual (coords en null) hasta que el administrador marque el punto en el mapa.
 * El origen de las coordenadas ({@link OrigenCoords}) distingue si las resolvió
 * ORS ({@code GEOCODING}) o el administrador ({@code MANUAL}).
 *
 * <p>No se inventan coordenadas: cambiar la dirección invalida las anteriores
 * hasta que se vuelvan a resolver.
 */
@Entity
@Table(name = "config_restaurante", schema = "delivery")
public class ConfigRestaurante extends AuditableEntity {

    /** Rango válido de latitud en grados decimales. */
    private static final BigDecimal LAT_MIN = BigDecimal.valueOf(-90);
    private static final BigDecimal LAT_MAX = BigDecimal.valueOf(90);
    /** Rango válido de longitud en grados decimales. */
    private static final BigDecimal LNG_MIN = BigDecimal.valueOf(-180);
    private static final BigDecimal LNG_MAX = BigDecimal.valueOf(180);

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(nullable = false, length = 200)
    private String direccion;

    @Column(precision = 10, scale = 7)
    private BigDecimal lat;

    @Column(precision = 10, scale = 7)
    private BigDecimal lng;

    @Column(name = "geocodificado_at")
    private Instant geocodificadoAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "origen_coords", length = 20)
    private OrigenCoords origenCoords;

    @Column(name = "fila_unica", nullable = false)
    private boolean filaUnica = true;

    protected ConfigRestaurante() {
    }

    public ConfigRestaurante(String direccion) {
        this.direccion = exigirDireccion(direccion);
    }

    /**
     * Cambia la dirección del establecimiento. Si la dirección cambia realmente,
     * las coordenadas vigentes quedan INVALIDADAS (pendientes de resolver de
     * nuevo): no se conservan coordenadas de una dirección distinta.
     */
    public void actualizarDireccion(String nuevaDireccion) {
        String limpia = exigirDireccion(nuevaDireccion);
        if (!limpia.equals(this.direccion)) {
            this.direccion = limpia;
            this.lat = null;
            this.lng = null;
            this.origenCoords = null;
            this.geocodificadoAt = null;
        }
    }

    /** Fija las coordenadas resueltas por ORS para la dirección actual. */
    public void fijarCoordenadasGeocoding(BigDecimal lat, BigDecimal lng) {
        fijarCoordenadas(lat, lng, OrigenCoords.GEOCODING);
    }

    /** Fija las coordenadas que el administrador marcó en el mapa. */
    public void fijarCoordenadasManual(BigDecimal lat, BigDecimal lng) {
        fijarCoordenadas(lat, lng, OrigenCoords.MANUAL);
    }

    public boolean tieneCoordenadas() {
        return lat != null && lng != null;
    }

    private void fijarCoordenadas(BigDecimal lat, BigDecimal lng, OrigenCoords origen) {
        validarRango(lat, lng);
        this.lat = lat;
        this.lng = lng;
        this.origenCoords = origen;
        this.geocodificadoAt = Instant.now();
    }

    private static String exigirDireccion(String direccion) {
        if (direccion == null || direccion.isBlank()) {
            throw new BusinessRuleException("La dirección del restaurante no puede estar vacía");
        }
        String limpia = direccion.strip();
        if (limpia.length() > 200) {
            throw new BusinessRuleException("La dirección del restaurante no puede superar 200 caracteres");
        }
        return limpia;
    }

    private static void validarRango(BigDecimal lat, BigDecimal lng) {
        if (lat == null || lng == null) {
            throw new BusinessRuleException("La ubicación necesita latitud y longitud");
        }
        if (lat.compareTo(LAT_MIN) < 0 || lat.compareTo(LAT_MAX) > 0) {
            throw new BusinessRuleException("La latitud debe estar entre -90 y 90 grados");
        }
        if (lng.compareTo(LNG_MIN) < 0 || lng.compareTo(LNG_MAX) > 0) {
            throw new BusinessRuleException("La longitud debe estar entre -180 y 180 grados");
        }
    }

    public String getDireccion() {
        return direccion;
    }

    public BigDecimal getLat() {
        return lat;
    }

    public BigDecimal getLng() {
        return lng;
    }

    public Instant getGeocodificadoAt() {
        return geocodificadoAt;
    }

    public OrigenCoords getOrigenCoords() {
        return origenCoords;
    }
}
