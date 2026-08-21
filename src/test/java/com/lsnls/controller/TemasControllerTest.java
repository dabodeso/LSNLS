package com.lsnls.controller;

import com.lsnls.service.TemasCatalogService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TemasControllerTest {

    @Mock
    private TemasCatalogService temasCatalogService;

    @InjectMocks
    private TemasController temasController;

    @Test
    void obtenerTemas_ok_devuelve200() {
        List<String> temas = Arrays.asList("HISTORIA", "CIENCIA");
        when(temasCatalogService.obtenerTematicasPreguntas()).thenReturn(temas);

        ResponseEntity<?> response = temasController.obtenerTemas();

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(temas, response.getBody());
    }

    @Test
    void obtenerTemas_excepcion_devuelve500() {
        when(temasCatalogService.obtenerTematicasPreguntas()).thenThrow(new RuntimeException("bd"));

        ResponseEntity<?> response = temasController.obtenerTemas();

        assertEquals(500, response.getStatusCodeValue());
        assertTrue(response.getBody().toString().contains("bd"));
    }

    @Test
    void obtenerSubtemas_ok_devuelve200() {
        List<String> subtemas = Collections.singletonList("ROMA");
        when(temasCatalogService.obtenerSubtemasPreguntas()).thenReturn(subtemas);

        ResponseEntity<?> response = temasController.obtenerSubtemas();

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(subtemas, response.getBody());
    }

    @Test
    void obtenerSubtemas_excepcion_devuelve500() {
        when(temasCatalogService.obtenerSubtemasPreguntas()).thenThrow(new RuntimeException("fail"));

        ResponseEntity<?> response = temasController.obtenerSubtemas();

        assertEquals(500, response.getStatusCodeValue());
        assertTrue(response.getBody().toString().contains("fail"));
    }

    @Test
    void añadirTema_vacio_devuelve400() {
        Map<String, String> request = new HashMap<>();
        request.put("tema", "  ");

        ResponseEntity<?> response = temasController.añadirTema(request);

        assertEquals(400, response.getStatusCodeValue());
        assertTrue(response.getBody().toString().contains("no puede estar vacío"));
    }

    @Test
    void añadirTema_nulo_devuelve400() {
        ResponseEntity<?> response = temasController.añadirTema(new HashMap<>());

        assertEquals(400, response.getStatusCodeValue());
    }

    @Test
    void añadirTema_ok_devuelve200() {
        when(temasCatalogService.añadirTematicaPregunta("historia")).thenReturn("HISTORIA");
        when(temasCatalogService.obtenerTematicasPreguntas()).thenReturn(Collections.singletonList("HISTORIA"));
        Map<String, String> request = new HashMap<>();
        request.put("tema", "historia");

        ResponseEntity<?> response = temasController.añadirTema(request);

        assertEquals(200, response.getStatusCodeValue());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("HISTORIA", body.get("tema"));
        assertEquals(1, body.get("totalTemas"));
    }

    @Test
    void añadirTema_excepcion_devuelve400() {
        when(temasCatalogService.añadirTematicaPregunta("X")).thenThrow(new RuntimeException("duplicado"));
        Map<String, String> request = new HashMap<>();
        request.put("tema", "X");

        ResponseEntity<?> response = temasController.añadirTema(request);

        assertEquals(400, response.getStatusCodeValue());
        assertTrue(response.getBody().toString().contains("duplicado"));
    }

    @Test
    void añadirSubtema_vacio_devuelve400() {
        Map<String, String> request = new HashMap<>();
        request.put("subtema", "");

        ResponseEntity<?> response = temasController.añadirSubtema(request);

        assertEquals(400, response.getStatusCodeValue());
        assertTrue(response.getBody().toString().contains("subtema no puede estar vacío"));
    }

    @Test
    void añadirSubtema_ok_devuelve200() {
        when(temasCatalogService.añadirSubtemaPregunta("roma")).thenReturn("ROMA");
        when(temasCatalogService.obtenerSubtemasPreguntas()).thenReturn(Collections.singletonList("ROMA"));
        Map<String, String> request = new HashMap<>();
        request.put("subtema", "roma");

        ResponseEntity<?> response = temasController.añadirSubtema(request);

        assertEquals(200, response.getStatusCodeValue());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("ROMA", body.get("subtema"));
    }

    @Test
    void añadirSubtema_excepcion_devuelve400() {
        when(temasCatalogService.añadirSubtemaPregunta("X")).thenThrow(new RuntimeException("error"));
        Map<String, String> request = new HashMap<>();
        request.put("subtema", "X");

        ResponseEntity<?> response = temasController.añadirSubtema(request);

        assertEquals(400, response.getStatusCodeValue());
    }

    @Test
    void eliminarTema_ok_devuelve200() {
        doNothing().when(temasCatalogService).eliminarTematicaPregunta("HISTORIA");
        when(temasCatalogService.obtenerTematicasPreguntas()).thenReturn(Collections.emptyList());

        ResponseEntity<?> response = temasController.eliminarTema(" historia ");

        assertEquals(200, response.getStatusCodeValue());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("HISTORIA", body.get("tema"));
    }

    @Test
    void eliminarTema_excepcion_devuelve400() {
        doThrow(new RuntimeException("en uso")).when(temasCatalogService).eliminarTematicaPregunta("X");

        ResponseEntity<?> response = temasController.eliminarTema("x");

        assertEquals(400, response.getStatusCodeValue());
        assertTrue(response.getBody().toString().contains("en uso"));
    }

    @Test
    void eliminarSubtema_ok_devuelve200() {
        doNothing().when(temasCatalogService).eliminarSubtemaPregunta("ROMA");
        when(temasCatalogService.obtenerSubtemasPreguntas()).thenReturn(Collections.emptyList());

        ResponseEntity<?> response = temasController.eliminarSubtema("roma");

        assertEquals(200, response.getStatusCodeValue());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("ROMA", body.get("subtema"));
    }

    @Test
    void eliminarSubtema_excepcion_devuelve400() {
        doThrow(new RuntimeException("fail")).when(temasCatalogService).eliminarSubtemaPregunta("X");

        ResponseEntity<?> response = temasController.eliminarSubtema("x");

        assertEquals(400, response.getStatusCodeValue());
    }

    @Test
    void obtenerEstadisticas_ok_devuelve200() {
        when(temasCatalogService.obtenerTematicasPreguntas()).thenReturn(Arrays.asList("A", "B"));
        when(temasCatalogService.obtenerSubtemasPreguntas()).thenReturn(Collections.singletonList("C"));

        ResponseEntity<?> response = temasController.obtenerEstadisticas();

        assertEquals(200, response.getStatusCodeValue());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals(2, body.get("totalTemas"));
        assertEquals(1, body.get("totalSubtemas"));
    }

    @Test
    void obtenerEstadisticas_excepcion_devuelve500() {
        when(temasCatalogService.obtenerTematicasPreguntas()).thenThrow(new RuntimeException("stats"));

        ResponseEntity<?> response = temasController.obtenerEstadisticas();

        assertEquals(500, response.getStatusCodeValue());
        assertTrue(response.getBody().toString().contains("stats"));
    }
}
