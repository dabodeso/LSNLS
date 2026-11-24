package com.lsnls.validation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.validation.ConstraintValidatorContext;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NoSpecialCharactersValidatorTest {

    private NoSpecialCharactersValidator validator;

    @Mock
    private ConstraintValidatorContext context;

    @Mock
    private NoSpecialCharacters annotation;

    @BeforeEach
    void setUp() {
        validator = new NoSpecialCharactersValidator();
        when(annotation.allowedChars()).thenReturn("");
        validator.initialize(annotation);
    }

    @Test
    void isValid_null_devuelveTrue() {
        assertTrue(validator.isValid(null, context));
    }

    @Test
    void isValid_textoBasico_devuelveTrue() {
        assertTrue(validator.isValid("Hola Mundo 123.,;:!?¡¿()[]\"'-", context));
    }

    @Test
    void isValid_acentosYEnie_devuelveTrue() {
        assertTrue(validator.isValid("NIÑO CANCIÓN", context));
    }

    @Test
    void isValid_simbolosHabituales_devuelveTrue() {
        assertTrue(validator.isValid("C++ / C# @ 50% #1 + 2*3", context));
    }

    @Test
    void isValid_saltoDeLinea_devuelveFalse() {
        assertFalse(validator.isValid("linea1\nlinea2", context));
    }

    @Test
    void isValid_caracterControl_devuelveFalse() {
        assertFalse(validator.isValid("hola\u0000mundo", context));
    }

    @Test
    void isValid_vacio_devuelveTrue() {
        assertTrue(validator.isValid("", context));
    }

    @Test
    void initialize_allowedCharsNull_noRompe() {
        when(annotation.allowedChars()).thenReturn(null);
        validator.initialize(annotation);
        assertTrue(validator.isValid("ABC", context));
    }
}
