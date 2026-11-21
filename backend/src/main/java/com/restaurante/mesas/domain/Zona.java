package com.restaurante.mesas.domain;

import com.restaurante.shared.domain.AuditableEntity;
import com.restaurante.shared.domain.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Zona del restaurante (Salón, Terraza, Patio…) definida por el administrador. Agrupa mesas y
 * define el orden en que las ve el mesero. No confundir con el área de preparación del catálogo.
 */
@Entity
@Table(name = "zonas", schema = "catalogo")
public class Zona extends AuditableEntity {

    public static final int MAX_NOMBRE = 60;

    private static final Pattern ESPACIOS = Pattern.compile("\\s+", Pattern.UNICODE_CHARACTER_CLASS);
    /** Marcas diacríticas, salvo la tilde (U+0303), para que "ñ" no se confunda con "n". */
    private static final Pattern MARCAS = Pattern.compile("[\\u0300-\\u0302\\u0304-\\u036f]");

    @Column(nullable = false, length = MAX_NOMBRE)
    private String nombre;

    @Column(nullable = false)
    private int orden;

    protected Zona() {
    }

    public Zona(String nombre, int orden) {
        this.nombre = normalizarNombre(nombre);
        this.orden = validarOrden(orden);
    }

    public void cambiarNombre(String nombre) {
        this.nombre = normalizarNombre(nombre);
    }

    public void cambiarOrden(int orden) {
        this.orden = validarOrden(orden);
    }

    public String getNombre() {
        return nombre;
    }

    public int getOrden() {
        return orden;
    }

    /** Recorta, compacta espacios y pone la primera letra en mayúscula; valida longitud. */
    public static String normalizarNombre(String texto) {
        String limpio = texto == null ? "" : ESPACIOS.matcher(texto).replaceAll(" ").strip();
        if (limpio.isEmpty()) {
            throw new BusinessRuleException("El nombre de la zona es obligatorio");
        }
        if (limpio.codePointCount(0, limpio.length()) > MAX_NOMBRE) {
            throw new BusinessRuleException("El nombre de la zona no puede superar " + MAX_NOMBRE + " caracteres");
        }
        int fin = limpio.offsetByCodePoints(0, 1);
        return limpio.substring(0, fin).toUpperCase(Locale.forLanguageTag("es")) + limpio.substring(fin);
    }

    /** Clave de comparación: dos nombres que solo difieren en mayúsculas, espacios o tildes son la misma zona. */
    public static String clave(String texto) {
        String compacto = texto == null ? "" : ESPACIOS.matcher(texto).replaceAll(" ").strip();
        String sinMarcas = MARCAS.matcher(Normalizer.normalize(compacto, Normalizer.Form.NFD)).replaceAll("");
        return Normalizer.normalize(sinMarcas, Normalizer.Form.NFC).toLowerCase(Locale.forLanguageTag("es"));
    }

    private static int validarOrden(int orden) {
        if (orden < 1) {
            throw new BusinessRuleException("El orden de la zona debe ser mayor que cero");
        }
        return orden;
    }
}
