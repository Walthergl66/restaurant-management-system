package com.restaurante.usuarios.infrastructure.correo;

import com.restaurante.usuarios.EmisorCorreo;
import com.restaurante.usuarios.PoliticaCorreos;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;

/**
 * Adaptador real de {@link EmisorCorreo} sobre la API transaccional de Brevo.
 * Solo existe como bean cuando {@code app.correo.brevo.habilitado=true}; en
 * desarrollo y pruebas lo reemplaza
 * {@link com.restaurante.usuarios.infrastructure.EmisorCorreoLog}.
 *
 * <p>El contenido sale de las plantillas versionadas en
 * {@code src/main/resources/correo}; Brevo solo transporta el mensaje.
 */
@Component
@ConditionalOnProperty(prefix = "app.correo.brevo", name = "habilitado", havingValue = "true")
public class EmisorCorreoBrevo implements EmisorCorreo {

    private final BrevoProperties propiedades;
    private final PlantillasCorreo plantillas;
    private final ClienteBrevo cliente;

    public EmisorCorreoBrevo(BrevoProperties propiedades, ObjectMapper objectMapper) {
        this.propiedades = propiedades;
        this.plantillas = new PlantillasCorreo();
        this.cliente = new ClienteBrevo(propiedades, objectMapper);
    }

    @Override
    public void enviarCodigoVerificacion(String correo, String codigo) {
        String restaurante = propiedades.getRemitenteNombre();
        long minutos = PoliticaCorreos.VERIFICACION.toMinutes();
        cliente.enviar(correo,
                "Verifica tu correo — " + restaurante,
                plantillas.verificacionHtml(restaurante, codigo, minutos),
                plantillas.verificacionTexto(restaurante, codigo, minutos),
                "verificacion-email");
    }

    @Override
    public void enviarRecuperacionPassword(String correo, String token, Duration vigencia) {
        String restaurante = propiedades.getRemitenteNombre();
        long minutos = vigencia.toMinutes();
        cliente.enviar(correo,
                "Recupera tu contraseña — " + restaurante,
                plantillas.recuperacionHtml(restaurante, token, minutos),
                plantillas.recuperacionTexto(restaurante, token, minutos),
                "recuperacion-password");
    }
}
