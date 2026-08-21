package com.lsnls.controller;

import com.lsnls.dto.ApiResponse;
import com.lsnls.service.UndoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UndoControllerTest {

    @Mock
    private UndoService undoService;

    @InjectMocks
    private UndoController undoController;

    @Test
    void deshacerOkDevuelve200() {
        when(undoService.deshacer(5L)).thenReturn("Borrar pregunta");

        ResponseEntity<ApiResponse<String>> respuesta = undoController.deshacer(5L);

        assertEquals(HttpStatus.OK, respuesta.getStatusCode());
        assertTrue(respuesta.getBody().isExito());
        assertEquals("Deshecho: Borrar pregunta", respuesta.getBody().getMensaje());
        assertEquals("Borrar pregunta", respuesta.getBody().getDatos());
    }

    @Test
    void deshacerSinDescripcionUsaMensajeGenerico() {
        when(undoService.deshacer(6L)).thenReturn("");

        ResponseEntity<ApiResponse<String>> respuesta = undoController.deshacer(6L);

        assertEquals(HttpStatus.OK, respuesta.getStatusCode());
        assertEquals("Operación deshecha", respuesta.getBody().getMensaje());
    }

    @Test
    void deshacerIllegalArgumentExceptionDevuelve400() {
        when(undoService.deshacer(7L)).thenThrow(new IllegalArgumentException("Solo puedes deshacer tus propias operaciones"));

        ResponseEntity<ApiResponse<String>> respuesta = undoController.deshacer(7L);

        assertEquals(HttpStatus.BAD_REQUEST, respuesta.getStatusCode());
        assertFalse(respuesta.getBody().isExito());
        assertEquals("Solo puedes deshacer tus propias operaciones", respuesta.getBody().getMensaje());
    }

    @Test
    void deshacerIllegalStateExceptionDevuelve400() {
        when(undoService.deshacer(8L)).thenThrow(new IllegalStateException("Tabla no permitida"));

        ResponseEntity<ApiResponse<String>> respuesta = undoController.deshacer(8L);

        assertEquals(HttpStatus.BAD_REQUEST, respuesta.getStatusCode());
        assertEquals("Tabla no permitida", respuesta.getBody().getMensaje());
    }

    @Test
    void deshacerExceptionDevuelve500() {
        when(undoService.deshacer(9L)).thenThrow(new RuntimeException("fallo interno"));

        ResponseEntity<ApiResponse<String>> respuesta = undoController.deshacer(9L);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, respuesta.getStatusCode());
        assertFalse(respuesta.getBody().isExito());
        assertTrue(respuesta.getBody().getMensaje().contains("Error al deshacer la operación"));
        assertTrue(respuesta.getBody().getMensaje().contains("fallo interno"));
    }
}
