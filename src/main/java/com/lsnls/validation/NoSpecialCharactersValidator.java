package com.lsnls.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class NoSpecialCharactersValidator implements ConstraintValidator<NoSpecialCharacters, String> {

    private String allowedChars;

    @Override
    public void initialize(NoSpecialCharacters constraintAnnotation) {
        this.allowedChars = constraintAnnotation.allowedChars();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // Permite valores nulos, otros validadores manejarán esto
        }
        
        // Patrón para caracteres permitidos:
        // - Letras (incluyendo acentos y ñ)
        // - Números
        // - Espacios
        // - Signos de puntuación básicos: .,;:!?¡¿()[]"'-
        // + caracteres adicionales especificados
        String allowedPattern = "[A-Za-zÀ-ÿÑñ0-9\\s.,;:!?¡¿()\\[\\]\"'\\-" + 
                               escapeRegexChars(allowedChars) + "]*";
        
        return value.matches(allowedPattern);
    }
    
    private String escapeRegexChars(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        // Escapa caracteres especiales de regex
        return input.replaceAll("([\\\\^\\$.*+?{}\\[\\]|()])", "\\\\$1");
    }
} 