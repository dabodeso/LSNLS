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
class NoLineBreaksValidatorTest {

    private NoLineBreaksValidator validator;

    @Mock
    private ConstraintValidatorContext context;

    @Mock
    private NoLineBreaks annotation;

    @BeforeEach
    void setUp() {
        validator = new NoLineBreaksValidator();
        validator.initialize(annotation);
    }

    @Test
    void isValid_null_devuelveTrue() {
        assertTrue(validator.isValid(null, context));
    }

    @Test
    void isValid_sinSaltos_devuelveTrue() {
        assertTrue(validator.isValid("texto en una linea", context));
    }

    @Test
    void isValid_vacio_devuelveTrue() {
        assertTrue(validator.isValid("", context));
    }

    @Test
    void isValid_conSaltoN_devuelveFalse() {
        assertFalse(validator.isValid("linea1\nlinea2", context));
    }

    @Test
    void isValid_conSaltoR_devuelveFalse() {
        assertFalse(validator.isValid("linea1\rlinea2", context));
    }

    @Test
    void isValid_conCRLF_devuelveFalse() {
        assertFalse(validator.isValid("linea1\r\nlinea2", context));
    }
}
