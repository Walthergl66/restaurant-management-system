package com.restaurante.usuarios;

import com.restaurante.AbstractIntegrationTest;
import com.restaurante.usuarios.infrastructure.EmisorCorreoLog;
import com.restaurante.usuarios.infrastructure.EmisorCorreoResend;
import com.restaurante.usuarios.infrastructure.MailProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * El interruptor en el sentido <b>encendido</b>: con {@code MAIL_ENABLED=true}
 * la aplicación monta el adaptador de API (no el de log) y liga las variables
 * del entorno en {@link MailProperties}. El sentido apagado lo cubre toda la
 * suite restante: cualquier otro contexto arranca con el default y necesita a
 * {@link EmisorCorreoLog} para inyectar el puerto en AuthService.
 */
@SpringBootTest(properties = {
        "spring.profiles.active=test",
        "app.mail.enabled=true",
        "app.mail.api-key=re_test_configuracion",
        "app.mail.from=onboarding@resend.dev",
        "app.mail.from-name=Restaurante Test"
})
class EmisorCorreoResendConfigTest extends AbstractIntegrationTest {

    @Autowired
    private ApplicationContext contexto;

    @Autowired
    private EmisorCorreo emisor;

    @Autowired
    private MailProperties mail;

    @Test
    void elInterruptorEncendidoMontaElAdaptadorDeApi() {
        assertInstanceOf(EmisorCorreoResend.class, emisor);
        assertTrue(contexto.getBeansOfType(EmisorCorreoLog.class).isEmpty(),
                "con MAIL_ENABLED=true no debe existir el adaptador de log");
    }

    @Test
    void lasVariablesDelEntornoSeLiganEnLasPropiedades() {
        assertTrue(mail.isEnabled());
        assertEquals("re_test_configuracion", mail.getApiKey());
        assertEquals("onboarding@resend.dev", mail.getFrom());
        assertEquals("Restaurante Test", mail.getFromName());
    }
}
