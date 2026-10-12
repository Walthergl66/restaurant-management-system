package com.restaurante.catalogo.domain;

import com.restaurante.shared.domain.exception.BusinessRuleException;
import java.util.Locale;
import java.util.regex.Pattern;

/** Regla compartida por nombres y descripciones; conserva marcas, siglas y tildes. */
public final class TextoCatalogo {
    public enum Tipo { NOMBRE, DESCRIPCION }

    private static final Pattern ESPACIOS = Pattern.compile("\\s+", Pattern.UNICODE_CHARACTER_CLASS);
    private static final Pattern ESPACIOS_HORIZONTALES = Pattern.compile("\\h+");

    private TextoCatalogo() {}

    public static String normalizar(String texto, Tipo tipo) {
        boolean nombre = tipo == Tipo.NOMBRE;
        Pattern espacios = nombre ? ESPACIOS : ESPACIOS_HORIZONTALES;
        String limpio = texto == null ? "" : espacios.matcher(texto).replaceAll(" ").strip();
        if (limpio.isEmpty()) {
            if (nombre) throw new BusinessRuleException("El nombre es obligatorio");
            return null;
        }
        int fin = limpio.offsetByCodePoints(0, 1);
        return limpio.substring(0, fin).toUpperCase(Locale.forLanguageTag("es")) + limpio.substring(fin);
    }
}
