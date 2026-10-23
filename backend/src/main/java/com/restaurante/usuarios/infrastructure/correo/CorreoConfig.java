package com.restaurante.usuarios.infrastructure.correo;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Registra la configuración de correo. El bean de {@link com.restaurante.usuarios.EmisorCorreo}
 * se elige por {@code app.correo.brevo.habilitado}: con {@code true} responde
 * {@link EmisorCorreoBrevo} y en cualquier otro caso
 * {@link com.restaurante.usuarios.infrastructure.EmisorCorreoLog}, de modo que
 * desarrollo y pruebas no necesitan credenciales.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(BrevoProperties.class)
public class CorreoConfig {
}
