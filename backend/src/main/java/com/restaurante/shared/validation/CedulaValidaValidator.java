package com.restaurante.shared.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/** Implementación de {@link CedulaValida}. */
public class CedulaValidaValidator implements ConstraintValidator<CedulaValida, String> {

    @Override
    public boolean isValid(String cedula, ConstraintValidatorContext context) {
        return ValidacionesEcuador.esCedulaValida(cedula);
    }
}
