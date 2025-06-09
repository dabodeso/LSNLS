package com.lsnls.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class UpperCaseValidator implements ConstraintValidator<UpperCase, String> {

    @Override
    public void initialize(UpperCase constraintAnnotation) {
        // No inicialización necesaria
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.trim().isEmpty()) {
            return true; // Permite valores nulos/vacíos, otros validadores manejarán esto
        }
        
        // Verifica que el texto esté completamente en mayúsculas
        return value.equals(value.toUpperCase());
    }
} 