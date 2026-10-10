package com.restaurante;

/**
 * Genera cédulas y celulares ecuatorianos válidos y únicos para los tests que
 * ejercitan el auto-registro de clientes (cada test usa su propia semilla).
 */
public final class SoporteRegistro {

    private SoporteRegistro() {
    }

    /** Cédula ecuatoriana válida derivada de una semilla (provincia 17). */
    public static String cedula(long semilla) {
        long n = Math.abs(semilla);
        String base = "17" + (n % 6) + String.format("%06d", n % 1_000_000);
        int[] coef = {2, 1, 2, 1, 2, 1, 2, 1, 2};
        int suma = 0;
        for (int i = 0; i < 9; i++) {
            int v = (base.charAt(i) - '0') * coef[i];
            if (v > 9) {
                v -= 9;
            }
            suma += v;
        }
        int verificador = (10 - (suma % 10)) % 10;
        return base + verificador;
    }

    /** Celular ecuatoriano válido (09 + 8 dígitos) derivado de una semilla. */
    public static String celular(long semilla) {
        long n = Math.abs(semilla);
        return "09" + String.format("%08d", n % 100_000_000L);
    }

    public static String correo(long semilla) {
        return "cliente_" + Math.abs(semilla) + "@test.com";
    }
}
