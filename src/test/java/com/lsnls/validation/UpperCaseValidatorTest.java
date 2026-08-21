package com.lsnls.validation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.validation.ConstraintValidatorContext;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class UpperCaseValidatorTest {

    private UpperCaseValidator validator;

    @Mock
    private ConstraintValidatorContext context;

    @Mock
    private UpperCase annotation;

    @BeforeEach
    void setUp() {
        validator = new UpperCaseValidator();
        validator.initialize(annotation);
    }

    @Test
    void isValid_null_devuelveTrue() {
        assertTrue(validator.isValid(null, context));
    }

    @Test
    void isValid_vacio_devuelveTrue() {
        assertTrue(validator.isValid("", context));
    }

    @Test
    void isValid_soloEspacios_devuelveTrue() {
        assertTrue(validator.isValid("   ", context));
    }

    @Test
    void isValid_todoMayusculas_devuelveTrue() {
        assertTrue(validator.isValid("HOLA MUNDO", context));
    }

    @Test
    void isValid_minusculas_devuelveFalse() {
        assertFalse(validator.isValid("hola", context));
    }

    @Test
    void isValid_mixto_devuelveFalse() {
        assertFalse(validator.isValid("Hola Mundo", context));
    }

    @Test
    void isValid_conNumerosYMayusculas_devuelveTrue() {
        assertTrue(validator.isValid("ABC123", context));
    }
}
