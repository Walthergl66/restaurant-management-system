package com.restaurante.shared.validation;

/**
 * Reglas de validación de documentos y contacto de Ecuador, sin dependencias
 * de Spring, reutilizables por constraints de Bean Validation y por el dominio.
 */
public final class ValidacionesEcuador {

    private ValidacionesEcuador() {
    }

    /**
     * Cédula ecuatoriana: 10 dígitos con dígito verificador (módulo 10).
     * Provincia 01-24 (o 30 para residentes en el exterior) y tercer dígito
     * 0-5 (persona natural).
     */
    public static boolean esCedulaValida(String cedula) {
        if (cedula == null) {
            return false;
        }
        String c = cedula.trim();
        if (c.length() != 10 || !c.chars().allMatch(Character::isDigit)) {
            return false;
        }
        int provincia = Integer.parseInt(c.substring(0, 2));
        if (provincia < 1 || (provincia > 24 && provincia != 30)) {
            return false;
        }
        int tercerDigito = c.charAt(2) - '0';
        if (tercerDigito > 5) {
            return false;
        }
        int[] coeficientes = {2, 1, 2, 1, 2, 1, 2, 1, 2};
        int suma = 0;
        for (int i = 0; i < 9; i++) {
            int valor = (c.charAt(i) - '0') * coeficientes[i];
            if (valor > 9) {
                valor -= 9;
            }
            suma += valor;
        }
        int verificador = (10 - (suma % 10)) % 10;
        return verificador == (c.charAt(9) - '0');
    }

    /** Celular de Ecuador: 10 dígitos que empiezan con 09. */
    public static boolean esCelularValido(String celular) {
        return celular != null && celular.trim().matches("09\\d{8}");
    }
}
