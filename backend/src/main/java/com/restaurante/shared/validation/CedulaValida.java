package com.restaurante.shared.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Valida una cédula ecuatoriana (10 dígitos con dígito verificador). */
@Documented
@Constraint(validatedBy = CedulaValidaValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT,
        ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface CedulaValida {

    String message() default "La cédula no es una cédula ecuatoriana válida";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
