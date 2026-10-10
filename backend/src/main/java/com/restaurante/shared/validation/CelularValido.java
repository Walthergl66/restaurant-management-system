package com.restaurante.shared.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Valida un celular de Ecuador (10 dígitos que empiezan con 09). */
@Documented
@Constraint(validatedBy = CelularValidoValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT,
        ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface CelularValido {

    String message() default "El celular debe tener 10 dígitos y empezar con 09";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
