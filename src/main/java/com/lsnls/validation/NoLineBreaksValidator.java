package com.lsnls.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class NoLineBreaksValidator implements ConstraintValidator<NoLineBreaks, String> {

    @Override
    public void initialize(NoLineBreaks constraintAnnotation) {
        // No inicialización necesaria
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // Permite valores nulos, otros validadores manejarán esto
        }
        
        // Verifica que no contenga saltos de línea (\n, \r, \r\n)
        return !value.contains("\n") && !value.contains("\r");
    }
} 