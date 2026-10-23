package com.restaurante.delivery.infrastructure;

import com.restaurante.delivery.domain.Coordenada;
import com.restaurante.delivery.domain.CriterioOptimizacion;
import com.restaurante.delivery.domain.GeocodingCache;
import com.restaurante.delivery.domain.ResultadoRuta;
import com.restaurante.delivery.domain.RutaCache;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;

/**
 * Servicio de rutas con CACHÉ: envoltura sobre {@link OrsClient} que añade la
 * reutilización necesaria para no gastar la cuota gratuita de ORS. La frontera
 * es deliberada: {@code OrsClient} es el proveedor (HTTP puro) y este servicio
 * es negocio + caché.
 *
 * <ul>
 *   <li>Geocoding: cacheado por {@code direccion_id}; se reusa mientras el
 *       texto de la dirección no cambie.</li>
 *   <li>Directions: cacheado por {@code clave_hash} (SHA-256 del origen, las
 *       paradas en orden y el criterio) con TTL de {@value #TTL_RUTA_MINUTOS}
 *       minutos.</li>
 *   <li>Optimization: se delega sin caché persistente (el orden óptimo depende
 *       del tráfico y se recalcula al planificar); no se repite dentro de la
 *       misma operación.</li>
 * </ul>
 */
@Service
@Transactional
public class RutasService {

    /** TTL de una ruta cacheada. Corto porque la geometría depende del tráfico. */
    static final long TTL_RUTA_MINUTOS = 15;

    private final OrsClient orsClient;
    private final GeocodingCacheRepository geocodingCache;
    private final RutaCacheRepository rutaCache;

    public RutasService(OrsClient orsClient, GeocodingCacheRepository geocodingCache,
                        RutaCacheRepository rutaCache) {
        this.orsClient = orsClient;
        this.geocodingCache = geocodingCache;
        this.rutaCache = rutaCache;
    }

    /**
     * Geocodifica una dirección reutilizando la caché, sin referencia de origen.
     */
    public Optional<Coordenada> geocodificar(Long direccionId, String textoDireccion) {
        return geocodificar(direccionId, textoDireccion, null);
    }

    /**
     * Geocodifica una dirección reutilizando la caché. Si existe una entrada
     * para {@code direccionId} con el mismo texto, la devuelve sin llamar a ORS,
     * salvo que esté demasiado lejos de {@code origenReferencia}: en ese caso se
     * descarta la caché y no se usa el resultado. Si no hay caché válida, llama
     * al proveedor, hace upsert y devuelve. Vacío si ORS no pudo geocodificar el
     * texto o el resultado no es plausible para el origen de reparto.
     */
    public Optional<Coordenada> geocodificar(Long direccionId, String textoDireccion,
                                             Coordenada origenReferencia) {
        Optional<GeocodingCache> cacheado = geocodingCache.findByDireccionId(direccionId);
        if (cacheado.isPresent() && textoCoincide(cacheado.get().getDireccionTexto(), textoDireccion)) {
            GeocodingCache c = cacheado.get();
            Coordenada coord = new Coordenada(c.getLat(), c.getLng());
            if (origenReferencia != null && distanciaKm(origenReferencia, coord) > DISTANCIA_MAXIMA_KM) {
                geocodingCache.delete(c);
                return Optional.empty();
            }
            return Optional.of(coord);
        }

        Optional<Coordenada> resuelto = orsClient.geocodeDireccion(textoDireccion);
        if (resuelto.isEmpty()) {
            return Optional.empty();
        }

        Coordenada coord = resuelto.get();
        if (origenReferencia != null && distanciaKm(origenReferencia, coord) > DISTANCIA_MAXIMA_KM) {
            return Optional.empty();
        }

        cacheado.ifPresent(geocodingCache::delete);
        geocodingCache.saveAndFlush(
                new GeocodingCache(direccionId, coord.lat(), coord.lng(), textoDireccion));
        return resuelto;
    }

    /** Distancia máxima plausible entre el origen de reparto y un destino geocodificado. */
    private static final double DISTANCIA_MAXIMA_KM = 150.0;

    private static double distanciaKm(Coordenada a, Coordenada b) {
        double r = 6371.0;
        double dLat = Math.toRadians(b.lat().doubleValue() - a.lat().doubleValue());
        double dLng = Math.toRadians(b.lng().doubleValue() - a.lng().doubleValue());
        double h = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(a.lat().doubleValue()))
                * Math.cos(Math.toRadians(b.lat().doubleValue()))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return 2 * r * Math.asin(Math.sqrt(h));
    }

    /**
     * Devuelve la ruta para las coordenadas en orden, reutilizando la caché por
     * {@code clave_hash} mientras no haya expirado. Si no hay entrada válida,
     * llama a ORS Directions, guarda el resultado con su {@code expira_at} y lo
     * devuelve.
     */
    public ResultadoRuta rutaPara(List<Coordenada> coordenadasEnOrden, CriterioOptimizacion criterio) {
        String claveHash = claveHash(coordenadasEnOrden, criterio);
        Instant ahora = Instant.now();

        Optional<RutaCache> cacheado = rutaCache.findByClaveHash(claveHash);
        if (cacheado.isPresent() && noExpiro(cacheado.get(), ahora)) {
            RutaCache r = cacheado.get();
            return new ResultadoRuta(r.getGeometria(), valor(r.getDistanciaM()), valor(r.getDuracionS()));
        }

        ResultadoRuta resultado = orsClient.calcularRuta(coordenadasEnOrden, criterio);
        cacheado.ifPresent(rutaCache::delete);
        rutaCache.saveAndFlush(new RutaCache(null, claveHash, resultado.geometria(),
                resultado.distanciaM(), resultado.duracionS(),
                ahora.plus(Duration.ofMinutes(TTL_RUTA_MINUTOS))));
        return resultado;
    }

    /**
     * Optimiza el orden de visita de los destinos desde el origen. Delega en el
     * proveedor; no persiste caché (el orden óptimo se recalcula al planificar).
     */
    public List<Integer> optimizar(Coordenada origen, List<Coordenada> destinos,
                                    CriterioOptimizacion criterio) {
        return orsClient.optimizarParadas(origen, destinos, criterio);
    }

    private static boolean noExpiro(RutaCache ruta, Instant ahora) {
        return ruta.getExpiraAt() == null || ruta.getExpiraAt().isAfter(ahora);
    }

    private static boolean textoCoincide(String cacheado, String actual) {
        return cacheado != null && cacheado.equals(actual);
    }

    private static int valor(Integer v) {
        return v == null ? 0 : v;
    }

    /**
     * Clave determinista del conjunto origen+paradas ordenadas+criterio: SHA-256
     * de una cadena canónica. El mismo recorrido produce siempre la misma clave;
     * cambiar el orden o el criterio produce una clave distinta.
     */
    static String claveHash(List<Coordenada> coordenadasEnOrden, CriterioOptimizacion criterio) {
        StringJoiner sj = new StringJoiner("|");
        for (Coordenada c : coordenadasEnOrden) {
            sj.add(canonico(c.lat()) + "," + canonico(c.lng()));
        }
        sj.add("crit=" + criterio.name());
        return sha256(sj.toString());
    }

    private static String canonico(BigDecimal valor) {
        // 7 decimales = la precisión que guarda GeocodingCache; evita que el
        // mismo punto genere claves distintas por ceros de más.
        return valor.setScale(7, java.math.RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }

    private static String sha256(String texto) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(texto.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16));
                sb.append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible en la JVM", e);
        }
    }
}
