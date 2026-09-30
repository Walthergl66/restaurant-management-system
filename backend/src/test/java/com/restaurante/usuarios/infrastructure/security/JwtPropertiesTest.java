package com.restaurante.usuarios.infrastructure.security;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * El secreto JWT no tiene valor por defecto en ningún perfil: si falta, viene
 * vacío o es corto, el arranque debe fallar en la validación de configuración
 * (y no más tarde, al firmar el primer token).
 */
class JwtPropertiesTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(Config.class);

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(JwtProperties.class)
    static class Config {
    }

    @Test
    void conSecretoValidoArranca() {
        runner.withPropertyValues("app.jwt.secret=" + "x".repeat(64))
                .run(ctx -> {
                    ctx.assertThat().hasNotFailed();
                    assertThat(ctx.getBean(JwtProperties.class).getSecret()).hasSize(64);
                });
    }

    @Test
    void sinSecretoFallaElArranque() {
        runner.run(ctx -> {
            ctx.assertThat().hasFailed();
            assertThat(cadena(ctx)).contains("app.jwt.secret").contains("JWT_SECRET es obligatorio");
        });
    }

    @Test
    void conSecretoVacioFallaElArranque() {
        runner.withPropertyValues("app.jwt.secret=")
                .run(ctx -> {
                    ctx.assertThat().hasFailed();
                    assertThat(cadena(ctx)).contains("JWT_SECRET es obligatorio");
                });
    }

    @Test
    void conSecretoCortoFallaElArranque() {
        runner.withPropertyValues("app.jwt.secret=clave-corta")
                .run(ctx -> {
                    ctx.assertThat().hasFailed();
                    assertThat(cadena(ctx)).contains("32 caracteres");
                });
    }

    /** El detalle de la violación de Bean Validation vive en la causa, no en el mensaje raíz. */
    private static String cadena(AssertableApplicationContext ctx) {
        StringBuilder sb = new StringBuilder();
        Throwable anterior = null;
        for (Throwable t = ctx.getStartupFailure(); t != null && t != anterior; t = t.getCause()) {
            sb.append(t).append('\n');
            anterior = t;
        }
        return sb.toString();
    }
}