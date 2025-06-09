package com.lsnls.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = UpperCaseValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface UpperCase {
    String message() default "El texto debe estar en mayúsculas";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
} 