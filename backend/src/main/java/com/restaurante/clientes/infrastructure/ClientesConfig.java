package com.restaurante.clientes.infrastructure;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de infraestructura del módulo clientes. Registra las
 * propiedades del canal de domicilio ({@link ClientesDeliveryProperties}).
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ClientesDeliveryProperties.class)
public class ClientesConfig {
}
