package com.lsnls.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiResponseTest {

    @Test
    void exitoso_conDatos_usaMensajePorDefecto() {
        ApiResponse<String> response = ApiResponse.exitoso("payload");

        assertTrue(response.isExito());
        assertEquals("Operación exitosa", response.getMensaje());
        assertEquals("payload", response.getDatos());
    }

    @Test
    void exitoso_conMensajeYDatos() {
        ApiResponse<Integer> response = ApiResponse.exitoso("Creado", 42);

        assertTrue(response.isExito());
        assertEquals("Creado", response.getMensaje());
        assertEquals(42, response.getDatos());
    }

    @Test
    void error_sinDatos() {
        ApiResponse<Object> response = ApiResponse.error("Fallo");

        assertFalse(response.isExito());
        assertEquals("Fallo", response.getMensaje());
        assertNull(response.getDatos());
    }

    @Test
    void constructoresLombok() {
        ApiResponse<String> vacio = new ApiResponse<>();
        vacio.setExito(true);
        vacio.setMensaje("ok");
        vacio.setDatos("d");
        assertTrue(vacio.isExito());

        ApiResponse<String> completo = new ApiResponse<>(false, "err", null);
        assertFalse(completo.isExito());
        assertEquals("err", completo.getMensaje());
        assertNull(completo.getDatos());
    }
}
