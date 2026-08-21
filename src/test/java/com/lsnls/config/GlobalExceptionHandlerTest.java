package com.lsnls.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void handleResponseStatus_conReason() {
        ResponseStatusException ex = new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe");

        ResponseEntity<Map<String, String>> response = handler.handleResponseStatus(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("No existe", response.getBody().get("mensaje"));
        assertEquals("No existe", response.getBody().get("message"));
    }

    @Test
    void handleResponseStatus_sinReason_usaMensajeClaro() {
        ResponseStatusException ex = new ResponseStatusException(HttpStatus.BAD_REQUEST);

        ResponseEntity<Map<String, String>> response = handler.handleResponseStatus(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(MensajesUsuario.VALIDACION, response.getBody().get("mensaje"));
    }

    @Test
    void handleResponseStatus_reasonTecnico_seSustituye() {
        ResponseStatusException ex = new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "could not execute statement; nested exception is java.sql.SQLException");

        ResponseEntity<Map<String, String>> response = handler.handleResponseStatus(ex);

        assertEquals(MensajesUsuario.GENERICO, response.getBody().get("mensaje"));
        assertFalse(response.getBody().get("mensaje").toLowerCase().contains("sql"));
    }

    @Test
    void handleMaxUploadSize() {
        ResponseEntity<Map<String, String>> response = handler.handleMaxUploadSize(new MaxUploadSizeExceededException(10));

        assertEquals(HttpStatus.PAYLOAD_TOO_LARGE, response.getStatusCode());
        assertEquals(MensajesUsuario.ARCHIVO_GRANDE, response.getBody().get("mensaje"));
    }

    @Test
    void handleAccessDenied() {
        ResponseEntity<Map<String, String>> response = handler.handleAccessDenied(new AccessDeniedException("denied"));

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals(MensajesUsuario.PERMISOS, response.getBody().get("mensaje"));
    }

    @Test
    void handleGeneric_nuncaDevuelveLaExcepcion() {
        ResponseEntity<Map<String, String>> response = handler.handleGeneric(
                new RuntimeException("org.hibernate.exception.ConstraintViolationException: duplicate"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(MensajesUsuario.GENERICO, response.getBody().get("mensaje"));
    }

    @Test
    void handleIllegalArgument_conservaMensajeDeNegocio() {
        ResponseEntity<Map<String, String>> response = handler.handleIllegalArgument(
                new IllegalArgumentException("El nombre es obligatorio"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("El nombre es obligatorio", response.getBody().get("mensaje"));
    }
}
