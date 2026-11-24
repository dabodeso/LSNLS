package com.lsnls.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

/**
 * Acepta texto usable en pregunta/respuesta/temática: letras, números,
 * puntuación y símbolos habituales (+, /, %, #, @, etc.).
 * Rechaza saltos de línea y caracteres de control.
 * {@code allowedChars} se conserva por compatibilidad y no restringe el texto.
 */
public class NoSpecialCharactersValidator implements ConstraintValidator<NoSpecialCharacters, String> {

    @Override
    public void initialize(NoSpecialCharacters constraintAnnotation) {
        // Se lee por compatibilidad; ya no restringe (importación/edición traen símbolos).
        if (constraintAnnotation != null) {
            constraintAnnotation.allowedChars();
        }
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // Otros validadores cubren null/blank
        }

        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '\n' || c == '\r') {
                return false;
            }
            if (Character.isISOControl(c) && !Character.isWhitespace(c)) {
                return false;
            }
        }
        return true;
    }
}
