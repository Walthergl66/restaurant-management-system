package com.restaurante.delivery.infrastructure;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * Configuración de infraestructura del proveedor de rutas OpenRouteService.
 * Define el {@link RestClient} del módulo (NO global): base-url y timeouts de
 * conexión y lectura salen de {@link OrsProperties}.
 *
 * <p>Se expone un {@link RestClient.Builder} ya configurado en vez del cliente
 * final para que {@link OrsClient} lo reciba por constructor y las pruebas
 * puedan enlazar el mismo builder a {@code MockRestServiceServer} sin red real.
 *
 * <p>La api-key NO se fija aquí como cabecera por defecto: puede estar vacía en
 * dev y {@link OrsClient} decide por petición si llama a ORS (fail-closed) y
 * añade la cabecera {@code Authorization}. Así nunca se envía una cabecera
 * vacía ni se filtra la clave.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({OrsProperties.class, DeliveryOrigenProperties.class})
public class OrsConfig {

    /** Nombre del bean para distinguirlo de cualquier otro RestClient.Builder. */
    public static final String ORS_REST_CLIENT_BUILDER = "orsRestClientBuilder";

    @Bean(ORS_REST_CLIENT_BUILDER)
    RestClient.Builder orsRestClientBuilder(OrsProperties propiedades) {
        Duration timeout = Duration.ofMillis(propiedades.getTimeoutMs());
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(timeout);
        requestFactory.setReadTimeout(timeout);
        return RestClient.builder()
                .baseUrl(propiedades.getBaseUrl())
                .requestFactory(requestFactory);
    }
}
