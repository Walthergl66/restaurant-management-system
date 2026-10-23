package com.restaurante.delivery.infrastructure;

import com.restaurante.delivery.domain.Coordenada;
import com.restaurante.delivery.domain.RutasNoDisponiblesException;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

/**
 * Origen de todas las rutas de reparto: la ubicación operativa del restaurante
 * ({@code app.delivery.origen}). Decisión de FASE 5: el origen se parametriza
 * por configuración de aplicación (no se hardcodea en el código de negocio) y
 * es sobreescribible por entorno ({@code DELIVERY_ORIGEN_LAT}/
 * {@code DELIVERY_ORIGEN_LNG}).
 *
 * <p>Se eligió esta vía en vez de una clave en el módulo {@code configuracion}
 * (schema configuracion / tabla parametros) porque las coordenadas del local
 * son configuración de despliegue estable, no un parámetro editable en caliente
 * por el administrador; mantenerlo en {@code application.yml} lo deja versionado
 * por entorno y evita acoplar delivery a la administración de parámetros. Si en
 * el futuro se quiere editar desde el panel, basta migrarlo a {@code Parametros}
 * sin tocar el servicio (ya recibe el origen por este puerto).
 *
 * <p>Sin coordenadas configuradas el módulo NO inventa un origen: responde
 * fail-closed (503 vía {@link RutasNoDisponiblesException}), igual que cuando
 * falta la api-key de ORS.
 */
@ConfigurationProperties(prefix = "app.delivery.origen")
public class DeliveryOrigenProperties {

    /** Latitud del restaurante en grados decimales. Vacía si no se configuró. */
    private BigDecimal lat;

    /** Longitud del restaurante en grados decimales. Vacía si no se configuró. */
    private BigDecimal lng;

    public boolean estaConfigurado() {
        return lat != null && lng != null;
    }

    /**
     * Coordenada del origen de rutas. Falla fail-closed si no se configuró: no
     * se inventa una ubicación del restaurante.
     */
    public Coordenada comoCoordenada() {
        if (!estaConfigurado()) {
            throw new RutasNoDisponiblesException(
                    "El origen de reparto (ubicación del restaurante) no está configurado");
        }
        return new Coordenada(lat, lng);
    }

    public BigDecimal getLat() {
        return lat;
    }

    public void setLat(BigDecimal lat) {
        this.lat = lat;
    }

    public BigDecimal getLng() {
        return lng;
    }

    public void setLng(BigDecimal lng) {
        this.lng = lng;
    }
}
