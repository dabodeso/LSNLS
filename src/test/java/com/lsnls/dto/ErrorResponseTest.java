package com.lsnls.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ErrorResponseTest {

    @Test
    void constructor_asignaCampos() {
        ErrorResponse response = new ErrorResponse("AUTH", "Credenciales inválidas");

        assertEquals("AUTH", response.getError());
        assertEquals("Credenciales inválidas", response.getMensaje());
    }

    @Test
    void setters() {
        ErrorResponse response = new ErrorResponse("A", "B");
        response.setError("C");
        response.setMensaje("D");
        assertEquals("C", response.getError());
        assertEquals("D", response.getMensaje());
    }
}
