package com.restaurante.shared.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Exige una contraseña fuerte: entre 8 y 72 caracteres con al menos una
 * minúscula, una mayúscula, un número y un símbolo.
 */
@Documented
@Constraint(validatedBy = PasswordSeguraValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT,
        ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface PasswordSegura {

    String message() default "La contraseña debe tener al menos 8 caracteres e incluir una "
            + "minúscula, una mayúscula, un número y un símbolo";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
