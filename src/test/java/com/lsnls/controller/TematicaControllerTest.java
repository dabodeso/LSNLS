package com.lsnls.controller;

import com.lsnls.entity.Tematica;
import com.lsnls.entity.Usuario;
import com.lsnls.service.AuthorizationService;
import com.lsnls.service.TematicaService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TematicaControllerTest {

    @Mock
    private TematicaService tematicaService;

    @Mock
    private AuthorizationService authService;

    @InjectMocks
    private TematicaController tematicaController;

    @Test
    void obtenerTodas_ok_devuelve200() {
        Tematica tematica = new Tematica();
        tematica.setId(1L);
        tematica.setNombre("HISTORIA");
        when(tematicaService.obtenerTodas()).thenReturn(Collections.singletonList(tematica));

        ResponseEntity<List<Tematica>> response = tematicaController.obtenerTodas();

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void obtenerTodas_excepcion_devuelve500() {
        when(tematicaService.obtenerTodas()).thenThrow(new RuntimeException("fail"));

        ResponseEntity<List<Tematica>> response = tematicaController.obtenerTodas();

        assertEquals(500, response.getStatusCodeValue());
        assertNull(response.getBody());
    }

    @Test
    void buscarPorTexto_ok_devuelve200() {
        when(tematicaService.buscarPorTexto("his")).thenReturn(Collections.emptyList());

        ResponseEntity<List<Tematica>> response = tematicaController.buscarPorTexto("his");

        assertEquals(200, response.getStatusCodeValue());
        assertTrue(response.getBody().isEmpty());
    }

    @Test
    void buscarPorTexto_excepcion_devuelve500() {
        when(tematicaService.buscarPorTexto("x")).thenThrow(new RuntimeException("fail"));

        ResponseEntity<List<Tematica>> response = tematicaController.buscarPorTexto("x");

        assertEquals(500, response.getStatusCodeValue());
    }

    @Test
    void crearTematica_nombreVacio_devuelve400() {
        Map<String, String> request = new HashMap<>();
        request.put("nombre", "  ");

        ResponseEntity<Map<String, Object>> response = tematicaController.crearTematica(request);

        assertEquals(400, response.getStatusCodeValue());
        assertEquals("El nombre de la temática es obligatorio", response.getBody().get("error"));
    }

    @Test
    void crearTematica_ok_devuelve200() {
        Tematica creada = new Tematica();
        creada.setId(1L);
        creada.setNombre("CIENCIA");
        when(tematicaService.crearTematica(eq("CIENCIA"), any(Usuario.class))).thenReturn(creada);
        Map<String, String> request = new HashMap<>();
        request.put("nombre", " CIENCIA ");

        ResponseEntity<Map<String, Object>> response = tematicaController.crearTematica(request);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(creada, response.getBody().get("tematica"));
    }

    @Test
    void crearTematica_excepcion_devuelve500() {
        when(tematicaService.crearTematica(eq("X"), any(Usuario.class)))
                .thenThrow(new RuntimeException("duplicada"));
        Map<String, String> request = new HashMap<>();
        request.put("nombre", "X");

        ResponseEntity<Map<String, Object>> response = tematicaController.crearTematica(request);

        assertEquals(500, response.getStatusCodeValue());
        assertTrue(response.getBody().get("error").toString().contains("duplicada"));
    }

    @Test
    void actualizarTematica_nombreVacio_devuelve400() {
        Map<String, String> request = new HashMap<>();
        request.put("nombre", "");

        ResponseEntity<Map<String, Object>> response = tematicaController.actualizarTematica(1L, request);

        assertEquals(400, response.getStatusCodeValue());
        assertEquals("El nombre de la temática es obligatorio", response.getBody().get("error"));
    }

    @Test
    void actualizarTematica_noEncontrada_devuelve404() {
        when(tematicaService.actualizarTematica(1L, "NUEVA")).thenReturn(null);
        Map<String, String> request = new HashMap<>();
        request.put("nombre", "NUEVA");

        ResponseEntity<Map<String, Object>> response = tematicaController.actualizarTematica(1L, request);

        assertEquals(404, response.getStatusCodeValue());
        assertNull(response.getBody());
    }

    @Test
    void actualizarTematica_ok_devuelve200() {
        Tematica actualizada = new Tematica();
        actualizada.setId(1L);
        actualizada.setNombre("NUEVA");
        when(tematicaService.actualizarTematica(1L, "NUEVA")).thenReturn(actualizada);
        Map<String, String> request = new HashMap<>();
        request.put("nombre", "NUEVA");

        ResponseEntity<Map<String, Object>> response = tematicaController.actualizarTematica(1L, request);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(actualizada, response.getBody().get("tematica"));
    }

    @Test
    void actualizarTematica_illegalArgument_devuelve400() {
        when(tematicaService.actualizarTematica(1L, "X")).thenThrow(new IllegalArgumentException("duplicada"));
        Map<String, String> request = new HashMap<>();
        request.put("nombre", "X");

        ResponseEntity<Map<String, Object>> response = tematicaController.actualizarTematica(1L, request);

        assertEquals(400, response.getStatusCodeValue());
        assertEquals("duplicada", response.getBody().get("error"));
    }

    @Test
    void actualizarTematica_excepcion_devuelve500() {
        when(tematicaService.actualizarTematica(1L, "X")).thenThrow(new RuntimeException("fail"));
        Map<String, String> request = new HashMap<>();
        request.put("nombre", "X");

        ResponseEntity<Map<String, Object>> response = tematicaController.actualizarTematica(1L, request);

        assertEquals(500, response.getStatusCodeValue());
        assertTrue(response.getBody().get("error").toString().contains("fail"));
    }

    @Test
    void eliminarTematica_noEncontrada_devuelve404() {
        when(tematicaService.eliminarTematica(9L)).thenReturn(false);

        ResponseEntity<Map<String, Object>> response = tematicaController.eliminarTematica(9L);

        assertEquals(404, response.getStatusCodeValue());
    }

    @Test
    void eliminarTematica_ok_devuelve200() {
        when(tematicaService.eliminarTematica(1L)).thenReturn(true);

        ResponseEntity<Map<String, Object>> response = tematicaController.eliminarTematica(1L);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals("Temática eliminada exitosamente", response.getBody().get("mensaje"));
    }

    @Test
    void eliminarTematica_excepcion_devuelve500() {
        when(tematicaService.eliminarTematica(1L)).thenThrow(new RuntimeException("en uso"));

        ResponseEntity<Map<String, Object>> response = tematicaController.eliminarTematica(1L);

        assertEquals(500, response.getStatusCodeValue());
        assertTrue(response.getBody().get("error").toString().contains("en uso"));
    }
}
