package com.restaurante.usuarios.infrastructure.correo;

import com.restaurante.usuarios.EmisorCorreo;
import com.restaurante.usuarios.infrastructure.EmisorCorreoLog;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Selección del emisor de correo por {@code app.correo.brevo.habilitado}: con
 * el envío apagado (dev y pruebas) el correo va al log y con Brevo encendido
 * hay exactamente un bean, el adaptador real.
 */
class EmisorCorreoSeleccionTest {

    private final ApplicationContextRunner contexto = new ApplicationContextRunner()
            .withUserConfiguration(CorreoConfig.class, EmisorCorreoLog.class, EmisorCorreoBrevo.class)
            .withBean(ObjectMapper.class, () -> JsonMapper.builder().build());

    @Test
    @DisplayName("sin la propiedad usa el emisor de log")
    void sinPropiedadUsaElLog() {
        contexto.run(ctx -> {
            assertThat(ctx.getBeansOfType(EmisorCorreo.class)).hasSize(1);
            assertThat(ctx.getBean(EmisorCorreo.class)).isInstanceOf(EmisorCorreoLog.class);
        });
    }

    @Test
    @DisplayName("con habilitado=false usa el emisor de log")
    void deshabilitadoUsaElLog() {
        contexto.withPropertyValues("app.correo.brevo.habilitado=false").run(ctx -> {
            assertThat(ctx.getBeansOfType(EmisorCorreo.class)).hasSize(1);
            assertThat(ctx.getBean(EmisorCorreo.class)).isInstanceOf(EmisorCorreoLog.class);
        });
    }

    @Test
    @DisplayName("con habilitado=true usa el adaptador de Brevo")
    void habilitadoUsaBrevo() {
        contexto.withPropertyValues(
                        "app.correo.brevo.habilitado=true",
                        "app.correo.brevo.api-key=clave-de-prueba",
                        "app.correo.brevo.remitente-email=no-reply@restaurante.test")
                .run(ctx -> {
                    assertThat(ctx.getBeansOfType(EmisorCorreo.class)).hasSize(1);
                    assertThat(ctx.getBean(EmisorCorreo.class)).isInstanceOf(EmisorCorreoBrevo.class);
                });
    }

    @Test
    @DisplayName("con habilitado=true sin credenciales el contexto no arranca")
    void habilitadoSinCredencialesFalla() {
        contexto.withPropertyValues("app.correo.brevo.habilitado=true")
                .run(ctx -> assertThat(ctx).hasFailed());
    }
}
