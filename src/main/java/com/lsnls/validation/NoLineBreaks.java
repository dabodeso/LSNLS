package com.lsnls.validation;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = NoLineBreaksValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface NoLineBreaks {
    String message() default "El texto no puede contener saltos de línea";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
} 