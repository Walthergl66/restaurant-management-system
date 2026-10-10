package com.restaurante.shared.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Implementación de {@link PasswordSegura}: una minúscula, una mayúscula, un
 * número y un símbolo, con longitud entre 8 y 72 caracteres (límite de BCrypt).
 */
public class PasswordSeguraValidator implements ConstraintValidator<PasswordSegura, String> {

    private static final int LONGITUD_MINIMA = 8;
    private static final int LONGITUD_MAXIMA = 72;

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (password == null) {
            return false;
        }
        if (password.length() < LONGITUD_MINIMA || password.length() > LONGITUD_MAXIMA) {
            return false;
        }
        boolean minuscula = false;
        boolean mayuscula = false;
        boolean numero = false;
        boolean simbolo = false;
        for (int i = 0; i < password.length(); i++) {
            char c = password.charAt(i);
            if (Character.isLowerCase(c)) {
                minuscula = true;
            } else if (Character.isUpperCase(c)) {
                mayuscula = true;
            } else if (Character.isDigit(c)) {
                numero = true;
            } else if (!Character.isWhitespace(c)) {
                simbolo = true;
            }
        }
        return minuscula && mayuscula && numero && simbolo;
    }
}
