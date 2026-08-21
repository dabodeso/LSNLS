package com.lsnls.config;

import com.lsnls.entity.Cuestionario.EstadoCuestionario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EstadoCuestionarioConverterTest {

    private EstadoCuestionarioConverter converter;

    @BeforeEach
    void setUp() {
        converter = new EstadoCuestionarioConverter();
    }

    @Test
    void convert_mayusculas() {
        assertEquals(EstadoCuestionario.borrador, converter.convert("BORRADOR"));
    }

    @Test
    void convert_minusculas() {
        assertEquals(EstadoCuestionario.aprobado, converter.convert("aprobado"));
    }

    @Test
    void convert_todosLosValores() {
        for (EstadoCuestionario estado : EstadoCuestionario.values()) {
            assertEquals(estado, converter.convert(estado.name()));
            assertEquals(estado, converter.convert(estado.name().toUpperCase()));
        }
    }

    @Test
    void convert_valorInvalido_devuelveNull() {
        assertNull(converter.convert("inexistente"));
    }

    @Test
    void convert_vacio_devuelveNull() {
        assertNull(converter.convert(""));
    }

    @Test
    void convert_null_lanzaNpe() {
        assertThrows(NullPointerException.class, () -> converter.convert(null));
    }
}
