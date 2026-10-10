package com.restaurante.shared.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/** Implementación de {@link CelularValido}. */
public class CelularValidoValidator implements ConstraintValidator<CelularValido, String> {

    @Override
    public boolean isValid(String celular, ConstraintValidatorContext context) {
        return ValidacionesEcuador.esCelularValido(celular);
    }
}
