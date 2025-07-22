package com.lsnls.validation;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = NoSpecialCharactersValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface NoSpecialCharacters {
    String message() default "El texto no puede contener caracteres especiales no permitidos";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
    
    String allowedChars() default ""; // Permite especificar caracteres adicionales permitidos
} 