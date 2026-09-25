package com.restaurante.clientes.infrastructure;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.Arrays;
import java.util.List;

/**
 * Broker simple STOMP para el canal en vivo del pedido del cliente (RF-43).
 * Endpoint de handshake {@code /ws} (permitido en SecurityConfig), destino de
 * suscripción {@code /topic/pedido/{codigo}} y prefijo de aplicación {@code /app}.
 * Or&iacute;genes permitidos (plan mejoras 3): la lista expl&iacute;cita de
 * {@code app.cors.allowed-origins} (obligatoria y sin `*` en el perfil prod);
 * el comod&iacute;n solo queda como default de desarrollo.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final StompJwtChannelInterceptor stompJwtChannelInterceptor;

    @Value("${app.cors.allowed-origins:*}")
    private String allowedOrigins;

    public WebSocketConfig(StompJwtChannelInterceptor stompJwtChannelInterceptor) {
        this.stompJwtChannelInterceptor = stompJwtChannelInterceptor;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        String[] origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toArray(String[]::new);
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(origins.length == 0 ? new String[]{"*"} : origins);
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(stompJwtChannelInterceptor);
    }
}