package com.restaurante.usuarios.infrastructure.correo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Validación de arranque de la configuración de Brevo: con el envío apagado no
 * se exige nada (dev y pruebas), y con el envío encendido la app se niega a
 * arrancar si falta la clave o el remitente.
 */
class BrevoPropertiesTest {

    @Test
    @DisplayName("deshabilitado no exige credenciales")
    void deshabilitadoNoExigeCredenciales() {
        BrevoProperties propiedades = new BrevoProperties();

        assertDoesNotThrow(propiedades::validar);
    }

    @Test
    @DisplayName("por defecto apunta al endpoint de Brevo con timeout de 5 segundos")
    void valoresPorDefecto() {
        BrevoProperties propiedades = new BrevoProperties();

        assertTrue(propiedades.getUrl().startsWith("https://api.brevo.com/"));
        assertEquals(Duration.ofSeconds(5), propiedades.getTimeout());
        assertEquals("Restaurante", propiedades.getRemitenteNombre());
    }

    @Test
    @DisplayName("con el envío activo la api-key es obligatoria")
    void apiKeyObligatoria() {
        BrevoProperties propiedades = habilitadas();
        propiedades.setApiKey("  ");

        IllegalStateException error = assertThrows(IllegalStateException.class, propiedades::validar);
        assertTrue(error.getMessage().contains("BREVO_API_KEY"));
    }

    @Test
    @DisplayName("con el envío activo el remitente es obligatorio y debe ser un correo")
    void remitenteObligatorioYValido() {
        BrevoProperties sinRemitente = habilitadas();
        sinRemitente.setRemitenteEmail("");
        assertThrows(IllegalStateException.class, sinRemitente::validar);

        BrevoProperties remitenteInvalido = habilitadas();
        remitenteInvalido.setRemitenteEmail("no-es-un-correo");
        IllegalStateException error = assertThrows(IllegalStateException.class,
                remitenteInvalido::validar);
        assertTrue(error.getMessage().contains("BREVO_REMITENTE_EMAIL"));
    }

    @Test
    @DisplayName("rechaza una URL mal formada y un timeout no positivo")
    void urlYTimeoutValidos() {
        BrevoProperties urlInvalida = habilitadas();
        urlInvalida.setUrl("http://url con espacios");
        assertThrows(IllegalStateException.class, urlInvalida::validar);

        BrevoProperties timeoutCero = habilitadas();
        timeoutCero.setTimeout(Duration.ZERO);
        assertThrows(IllegalStateException.class, timeoutCero::validar);
    }

    private BrevoProperties habilitadas() {
        BrevoProperties propiedades = new BrevoProperties();
        propiedades.setHabilitado(true);
        propiedades.setApiKey("clave");
        propiedades.setRemitenteEmail("no-reply@restaurante.test");
        return propiedades;
    }
}
