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
    }

    private void inicializar(String allowedChars) {
        when(annotation.allowedChars()).thenReturn(allowedChars);
        validator.initialize(annotation);
    }

    @Test
    void isValid_null_devuelveTrue() {
        inicializar("");
        assertTrue(validator.isValid(null, context));
    }

    @Test
    void isValid_textoBasico_conAllowedVacio_devuelveTrue() {
        inicializar("");
        assertTrue(validator.isValid("Hola Mundo 123.,;:!?¡¿()[]\"'-", context));
    }

    @Test
    void isValid_acentosYEnie_devuelveTrue() {
        inicializar("");
        assertTrue(validator.isValid("NIÑO CANCIÓN", context));
    }

    @Test
    void isValid_arrobaSinPermitir_devuelveFalse() {
        inicializar("");
        assertFalse(validator.isValid("correo@dominio", context));
    }

    @Test
    void isValid_arrobaPermitida_devuelveTrue() {
        inicializar("@");
        assertTrue(validator.isValid("correo@dominio", context));
    }

    @Test
    void isValid_hashSinPermitir_devuelveFalse() {
        inicializar("");
        assertFalse(validator.isValid("tema #1", context));
    }

    @Test
    void isValid_vacio_devuelveTrue() {
        inicializar("");
        assertTrue(validator.isValid("", context));
    }

    @Test
    void initialize_allowedCharsNull_noRompe() {
        when(annotation.allowedChars()).thenReturn(null);
        validator.initialize(annotation);
        assertTrue(validator.isValid("ABC", context));
    }

    @Test
    void isValid_caracterRegexEscapadoEnAllowed() {
        inicializar(".*");
        assertTrue(validator.isValid("abc.def*", context));
    }
}
