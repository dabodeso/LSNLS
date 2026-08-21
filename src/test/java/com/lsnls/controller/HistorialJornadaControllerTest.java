package com.lsnls.controller;

import com.lsnls.dto.ApiResponse;
import com.lsnls.dto.HistorialJornadaDTO;
import com.lsnls.dto.MarcarNoUsadoDTO;
import com.lsnls.dto.ReaprovecharComboDTO;
import com.lsnls.entity.Combo;
import com.lsnls.service.AuthorizationService;
import com.lsnls.service.HistorialJornadaService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HistorialJornadaControllerTest {

    @Mock
    private HistorialJornadaService historialService;

    @Mock
    private AuthorizationService authService;

    @InjectMocks
    private HistorialJornadaController historialJornadaController;

    @Test
    void obtenerHistorialCuestionario_ok_devuelve200() {
        List<HistorialJornadaDTO> historial = Collections.singletonList(new HistorialJornadaDTO());
        when(historialService.obtenerHistorialCuestionario(1L)).thenReturn(historial);

        ResponseEntity<ApiResponse<List<HistorialJornadaDTO>>> response =
                historialJornadaController.obtenerHistorialCuestionario(1L);

        assertEquals(200, response.getStatusCodeValue());
        assertTrue(response.getBody().isExito());
        assertEquals(historial, response.getBody().getDatos());
    }

    @Test
    void obtenerHistorialCuestionario_excepcion_devuelve500() {
        when(historialService.obtenerHistorialCuestionario(1L)).thenThrow(new RuntimeException("fail"));

        ResponseEntity<ApiResponse<List<HistorialJornadaDTO>>> response =
                historialJornadaController.obtenerHistorialCuestionario(1L);

        assertEquals(500, response.getStatusCodeValue());
        assertFalse(response.getBody().isExito());
    }

    @Test
    void obtenerHistorialCombo_ok_devuelve200() {
        when(historialService.obtenerHistorialCombo(2L)).thenReturn(Collections.emptyList());

        ResponseEntity<ApiResponse<List<HistorialJornadaDTO>>> response =
                historialJornadaController.obtenerHistorialCombo(2L);

        assertEquals(200, response.getStatusCodeValue());
        assertTrue(response.getBody().getDatos().isEmpty());
    }

    @Test
    void obtenerHistorialCombo_excepcion_devuelve500() {
        when(historialService.obtenerHistorialCombo(2L)).thenThrow(new RuntimeException("fail"));

        ResponseEntity<ApiResponse<List<HistorialJornadaDTO>>> response =
                historialJornadaController.obtenerHistorialCombo(2L);

        assertEquals(500, response.getStatusCodeValue());
    }

    @Test
    void obtenerNoUsados_ok_devuelve200() {
        when(historialService.obtenerNoUsados(3L)).thenReturn(Collections.emptyList());

        ResponseEntity<ApiResponse<List<HistorialJornadaDTO>>> response =
                historialJornadaController.obtenerNoUsados(3L);

        assertEquals(200, response.getStatusCodeValue());
        assertTrue(response.getBody().isExito());
    }

    @Test
    void obtenerNoUsados_excepcion_devuelve500() {
        when(historialService.obtenerNoUsados(3L)).thenThrow(new RuntimeException("fail"));

        ResponseEntity<ApiResponse<List<HistorialJornadaDTO>>> response =
                historialJornadaController.obtenerNoUsados(3L);

        assertEquals(500, response.getStatusCodeValue());
    }

    @Test
    void marcarNoUsados_jornadaNula_devuelve400() {
        MarcarNoUsadoDTO dto = new MarcarNoUsadoDTO();
        dto.setCuestionarioIds(Collections.singletonList(1L));

        ResponseEntity<ApiResponse<String>> response = historialJornadaController.marcarNoUsados(dto);

        assertEquals(400, response.getStatusCodeValue());
        assertTrue(response.getBody().getMensaje().contains("jornada"));
    }

    @Test
    void marcarNoUsados_sinElementos_devuelve400() {
        MarcarNoUsadoDTO dto = new MarcarNoUsadoDTO();
        dto.setJornadaId(1L);

        ResponseEntity<ApiResponse<String>> response = historialJornadaController.marcarNoUsados(dto);

        assertEquals(400, response.getStatusCodeValue());
        assertTrue(response.getBody().getMensaje().contains("al menos un cuestionario o combo"));
    }

    @Test
    void marcarNoUsados_ok_devuelve200() {
        MarcarNoUsadoDTO dto = new MarcarNoUsadoDTO();
        dto.setJornadaId(1L);
        dto.setCuestionarioIds(Collections.singletonList(10L));
        dto.setComboIds(Collections.singletonList(20L));
        doNothing().when(historialService).marcarNoUsados(dto);

        ResponseEntity<ApiResponse<String>> response = historialJornadaController.marcarNoUsados(dto);

        assertEquals(200, response.getStatusCodeValue());
        assertTrue(response.getBody().getMensaje().contains("Cuestionarios: 1"));
        assertTrue(response.getBody().getMensaje().contains("Combos: 1"));
    }

    @Test
    void marcarNoUsados_excepcion_devuelve500() {
        MarcarNoUsadoDTO dto = new MarcarNoUsadoDTO();
        dto.setJornadaId(1L);
        dto.setCuestionarioIds(Collections.singletonList(10L));
        doThrow(new RuntimeException("fail")).when(historialService).marcarNoUsados(dto);

        ResponseEntity<ApiResponse<String>> response = historialJornadaController.marcarNoUsados(dto);

        assertEquals(500, response.getStatusCodeValue());
    }

    @Test
    void reaprovecharCombo_comboNulo_devuelve400() {
        ReaprovecharComboDTO dto = new ReaprovecharComboDTO();
        dto.setPreguntaUsadaId(1L);
        dto.setNuevaPreguntaId(2L);

        ResponseEntity<ApiResponse<Combo>> response = historialJornadaController.reaprovecharCombo(dto);

        assertEquals(400, response.getStatusCodeValue());
        assertTrue(response.getBody().getMensaje().contains("combo original"));
    }

    @Test
    void reaprovecharCombo_preguntaUsadaNula_devuelve400() {
        ReaprovecharComboDTO dto = new ReaprovecharComboDTO();
        dto.setComboOriginalId(1L);
        dto.setNuevaPreguntaId(2L);

        ResponseEntity<ApiResponse<Combo>> response = historialJornadaController.reaprovecharCombo(dto);

        assertEquals(400, response.getStatusCodeValue());
        assertTrue(response.getBody().getMensaje().contains("pregunta usada"));
    }

    @Test
    void reaprovecharCombo_nuevaPreguntaNula_devuelve400() {
        ReaprovecharComboDTO dto = new ReaprovecharComboDTO();
        dto.setComboOriginalId(1L);
        dto.setPreguntaUsadaId(2L);

        ResponseEntity<ApiResponse<Combo>> response = historialJornadaController.reaprovecharCombo(dto);

        assertEquals(400, response.getStatusCodeValue());
        assertTrue(response.getBody().getMensaje().contains("nueva pregunta"));
    }

    @Test
    void reaprovecharCombo_ok_devuelve200() {
        ReaprovecharComboDTO dto = new ReaprovecharComboDTO();
        dto.setComboOriginalId(1L);
        dto.setPreguntaUsadaId(2L);
        dto.setNuevaPreguntaId(3L);
        Combo nuevo = new Combo();
        nuevo.setId(99L);
        when(historialService.reaprovecharCombo(dto)).thenReturn(nuevo);

        ResponseEntity<ApiResponse<Combo>> response = historialJornadaController.reaprovecharCombo(dto);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(99L, response.getBody().getDatos().getId());
        assertTrue(response.getBody().getMensaje().contains("99"));
    }

    @Test
    void reaprovecharCombo_excepcion_devuelve500() {
        ReaprovecharComboDTO dto = new ReaprovecharComboDTO();
        dto.setComboOriginalId(1L);
        dto.setPreguntaUsadaId(2L);
        dto.setNuevaPreguntaId(3L);
        when(historialService.reaprovecharCombo(dto)).thenThrow(new RuntimeException("fail"));

        ResponseEntity<ApiResponse<Combo>> response = historialJornadaController.reaprovecharCombo(dto);

        assertEquals(500, response.getStatusCodeValue());
    }

    @Test
    void asignarCuestionario_ok_devuelve200() {
        when(historialService.registrarAsignacionCuestionario(1L, 2L)).thenReturn(null);

        ResponseEntity<ApiResponse<String>> response = historialJornadaController.asignarCuestionario(1L, 2L);

        assertEquals(200, response.getStatusCodeValue());
        assertTrue(response.getBody().isExito());
    }

    @Test
    void asignarCuestionario_excepcion_devuelve500() {
        doThrow(new RuntimeException("fail")).when(historialService).registrarAsignacionCuestionario(1L, 2L);

        ResponseEntity<ApiResponse<String>> response = historialJornadaController.asignarCuestionario(1L, 2L);

        assertEquals(500, response.getStatusCodeValue());
    }

    @Test
    void asignarCombo_ok_devuelve200() {
        when(historialService.registrarAsignacionCombo(1L, 5L)).thenReturn(null);

        ResponseEntity<ApiResponse<String>> response = historialJornadaController.asignarCombo(1L, 5L);

        assertEquals(200, response.getStatusCodeValue());
        assertTrue(response.getBody().isExito());
    }

    @Test
    void asignarCombo_excepcion_devuelve500() {
        doThrow(new RuntimeException("fail")).when(historialService).registrarAsignacionCombo(1L, 5L);

        ResponseEntity<ApiResponse<String>> response = historialJornadaController.asignarCombo(1L, 5L);

        assertEquals(500, response.getStatusCodeValue());
    }
}
