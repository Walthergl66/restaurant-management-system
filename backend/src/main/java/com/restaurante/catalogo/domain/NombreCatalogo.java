package com.restaurante.catalogo.domain;

import com.restaurante.shared.domain.exception.BusinessRuleException;
import java.util.Locale;
import java.util.regex.Pattern;

/** Limpia espacios y capitaliza únicamente la primera letra; conserva marcas y siglas. */
public final class NombreCatalogo {
    private static final Pattern ESPACIOS = Pattern.compile("\\s+", Pattern.UNICODE_CHARACTER_CLASS);

    private NombreCatalogo() {}

    public static String normalizar(String nombre) {
        if (nombre == null) throw new BusinessRuleException("El nombre es obligatorio");
        String limpio = ESPACIOS.matcher(nombre).replaceAll(" ").strip();
        if (limpio.isEmpty()) throw new BusinessRuleException("El nombre es obligatorio");
        int fin = limpio.offsetByCodePoints(0, 1);
        return limpio.substring(0, fin).toUpperCase(Locale.forLanguageTag("es")) + limpio.substring(fin);
    }
}
