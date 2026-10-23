package com.restaurante.usuarios.infrastructure.correo;

import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Plantillas del correo transaccional versionadas en el repositorio
 * ({@code src/main/resources/correo}). Se cargan una vez y se rellenan con
 * placeholders {@code {{nombre}}}. El contenido no depende del panel de Brevo,
 * así que se revisa en git y es el mismo en todos los entornos.
 */
public class PlantillasCorreo {

    private final String verificacionHtml;
    private final String verificacionTexto;
    private final String recuperacionHtml;
    private final String recuperacionTexto;

    public PlantillasCorreo() {
        this.verificacionHtml = cargar("verificacion.html");
        this.verificacionTexto = cargar("verificacion.txt");
        this.recuperacionHtml = cargar("recuperacion.html");
        this.recuperacionTexto = cargar("recuperacion.txt");
    }

    public String verificacionHtml(String restaurante, String codigo, long vigenciaMinutos) {
        return rellenar(verificacionHtml, true, Map.of(
                "restaurante", restaurante,
                "codigo", codigo,
                "vigenciaMinutos", Long.toString(vigenciaMinutos)));
    }

    public String verificacionTexto(String restaurante, String codigo, long vigenciaMinutos) {
        return rellenar(verificacionTexto, false, Map.of(
                "restaurante", restaurante,
                "codigo", codigo,
                "vigenciaMinutos", Long.toString(vigenciaMinutos)));
    }

    public String recuperacionHtml(String restaurante, String token, long vigenciaMinutos) {
        return rellenar(recuperacionHtml, true, Map.of(
                "restaurante", restaurante,
                "token", token,
                "vigenciaMinutos", Long.toString(vigenciaMinutos)));
    }

    public String recuperacionTexto(String restaurante, String token, long vigenciaMinutos) {
        return rellenar(recuperacionTexto, false, Map.of(
                "restaurante", restaurante,
                "token", token,
                "vigenciaMinutos", Long.toString(vigenciaMinutos)));
    }

    private static String cargar(String nombre) {
        try {
            return new ClassPathResource("correo/" + nombre).getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo cargar la plantilla de correo " + nombre, e);
        }
    }

    private static String rellenar(String plantilla, boolean html, Map<String, String> valores) {
        String resultado = plantilla;
        for (Map.Entry<String, String> valor : valores.entrySet()) {
            String texto = html ? escaparHtml(valor.getValue()) : valor.getValue();
            resultado = resultado.replace("{{" + valor.getKey() + "}}", texto);
        }
        return resultado;
    }

    private static String escaparHtml(String valor) {
        return valor.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
