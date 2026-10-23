package com.restaurante.shared.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Implementación de {@link CelularValido}. Siguiendo la convención de Bean
 * Validation, un valor nulo o en blanco se considera VÁLIDO: la obligatoriedad
 * se expresa con {@code @NotBlank} en los campos que la requieren (p. ej. el
 * celular del repartidor). Así el mismo constraint sirve para un campo opcional
 * (cliente) validando solo el formato cuando el valor está presente.
 */
public class CelularValidoValidator implements ConstraintValidator<CelularValido, String> {

    @Override
    public boolean isValid(String celular, ConstraintValidatorContext context) {
        if (celular == null || celular.isBlank()) {
            return true;
        }
        return ValidacionesEcuador.esCelularValido(celular);
    }
}
