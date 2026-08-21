package com.lsnls.controller;

import com.lsnls.entity.Combo;
import com.lsnls.entity.Concursante;
import com.lsnls.entity.Cuestionario;
import com.lsnls.entity.Jornada;
import com.lsnls.entity.Pregunta;
import com.lsnls.entity.Programa;
import com.lsnls.repository.ConcursanteRepository;
import com.lsnls.repository.JornadaRepository;
import com.lsnls.service.ComboService;
import com.lsnls.service.CuestionarioService;
import com.lsnls.service.PreguntaService;
import com.lsnls.service.ProgramaService;
import com.lsnls.service.ValidationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ValidationControllerTest {

    @Mock
    private ValidationService validationService;
    @Mock
    private PreguntaService preguntaService;
    @Mock
    private CuestionarioService cuestionarioService;
    @Mock
    private ComboService comboService;
    @Mock
    private ConcursanteRepository concursanteRepository;
    @Mock
    private JornadaRepository jornadaRepository;
    @Mock
    private ProgramaService programaService;

    @InjectMocks
    private ValidationController validationController;

    private ValidationService.ValidationResult valido() {
        return new ValidationService.ValidationResult();
    }

    private ValidationService.ValidationResult invalido() {
        ValidationService.ValidationResult result = new ValidationService.ValidationResult();
        result.addError("error crítico");
        result.addWarning("aviso");
        return result;
    }

    @Test
    void validarPregunta_idNulo_devuelve400() {
        ResponseEntity<?> response = validationController.validarPregunta(null);

        assertEquals(400, response.getStatusCodeValue());
        assertTrue(response.getBody().toString().contains("requerido"));
    }

    @Test
    void validarPregunta_noEncontrada_devuelve404() {
        when(preguntaService.obtenerPorId(9L)).thenReturn(Optional.empty());

        ResponseEntity<?> response = validationController.validarPregunta(9L);

        assertEquals(404, response.getStatusCodeValue());
    }

    @Test
    void validarPregunta_ok_devuelve200() {
        Pregunta pregunta = new Pregunta();
        pregunta.setId(1L);
        when(preguntaService.obtenerPorId(1L)).thenReturn(Optional.of(pregunta));
        when(validationService.validarIntegridadPregunta(pregunta)).thenReturn(valido());

        ResponseEntity<?> response = validationController.validarPregunta(1L);

        assertEquals(200, response.getStatusCodeValue());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("Pregunta", body.get("entidad"));
        assertEquals(true, body.get("valida"));
    }

    @Test
    void validarPregunta_excepcion_devuelve400() {
        when(preguntaService.obtenerPorId(1L)).thenThrow(new RuntimeException("fail"));

        ResponseEntity<?> response = validationController.validarPregunta(1L);

        assertEquals(400, response.getStatusCodeValue());
        assertTrue(response.getBody().toString().contains("fail"));
    }

    @Test
    void validarCuestionario_noEncontrado_devuelve404() {
        when(cuestionarioService.obtenerConPreguntas(9L)).thenReturn(Optional.empty());

        ResponseEntity<?> response = validationController.validarCuestionario(9L);

        assertEquals(404, response.getStatusCodeValue());
        assertNull(response.getBody());
    }

    @Test
    void validarCuestionario_ok_devuelve200() {
        Cuestionario c = new Cuestionario();
        c.setId(1L);
        when(cuestionarioService.obtenerConPreguntas(1L)).thenReturn(Optional.of(c));
        when(validationService.validarIntegridadCuestionario(c)).thenReturn(invalido());

        ResponseEntity<?> response = validationController.validarCuestionario(1L);

        assertEquals(200, response.getStatusCodeValue());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals(false, body.get("valida"));
    }

    @Test
    void validarCuestionario_excepcion_devuelve400() {
        when(cuestionarioService.obtenerConPreguntas(1L)).thenThrow(new RuntimeException("fail"));

        ResponseEntity<?> response = validationController.validarCuestionario(1L);

        assertEquals(400, response.getStatusCodeValue());
    }

    @Test
    void validarCombo_noEncontrado_devuelve404() {
        when(comboService.obtenerConPreguntas(9L)).thenReturn(Optional.empty());

        ResponseEntity<?> response = validationController.validarCombo(9L);

        assertEquals(404, response.getStatusCodeValue());
    }

    @Test
    void validarCombo_ok_devuelve200() {
        Combo combo = new Combo();
        combo.setId(1L);
        when(comboService.obtenerConPreguntas(1L)).thenReturn(Optional.of(combo));
        when(validationService.validarIntegridadCombo(combo)).thenReturn(valido());

        ResponseEntity<?> response = validationController.validarCombo(1L);

        assertEquals(200, response.getStatusCodeValue());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("Combo", body.get("entidad"));
    }

    @Test
    void validarCombo_excepcion_devuelve400() {
        when(comboService.obtenerConPreguntas(1L)).thenThrow(new RuntimeException("fail"));

        ResponseEntity<?> response = validationController.validarCombo(1L);

        assertEquals(400, response.getStatusCodeValue());
    }

    @Test
    void validarConcursante_noEncontrado_devuelve404() {
        when(concursanteRepository.findById(9L)).thenReturn(Optional.empty());

        ResponseEntity<?> response = validationController.validarConcursante(9L);

        assertEquals(404, response.getStatusCodeValue());
    }

    @Test
    void validarConcursante_ok_devuelve200() {
        Concursante c = new Concursante();
        when(concursanteRepository.findById(1L)).thenReturn(Optional.of(c));
        when(validationService.validarIntegridadConcursante(c)).thenReturn(valido());

        ResponseEntity<?> response = validationController.validarConcursante(1L);

        assertEquals(200, response.getStatusCodeValue());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("Concursante", body.get("entidad"));
    }

    @Test
    void validarConcursante_excepcion_devuelve400() {
        when(concursanteRepository.findById(1L)).thenThrow(new RuntimeException("fail"));

        ResponseEntity<?> response = validationController.validarConcursante(1L);

        assertEquals(400, response.getStatusCodeValue());
    }

    @Test
    void validarJornada_noEncontrada_devuelve404() {
        when(jornadaRepository.findById(9L)).thenReturn(Optional.empty());

        ResponseEntity<?> response = validationController.validarJornada(9L);

        assertEquals(404, response.getStatusCodeValue());
    }

    @Test
    void validarJornada_ok_devuelve200() {
        Jornada j = new Jornada();
        j.setId(1L);
        when(jornadaRepository.findById(1L)).thenReturn(Optional.of(j));
        when(validationService.validarIntegridadJornada(j)).thenReturn(valido());

        ResponseEntity<?> response = validationController.validarJornada(1L);

        assertEquals(200, response.getStatusCodeValue());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("Jornada", body.get("entidad"));
    }

    @Test
    void validarJornada_excepcion_devuelve400() {
        when(jornadaRepository.findById(1L)).thenThrow(new RuntimeException("fail"));

        ResponseEntity<?> response = validationController.validarJornada(1L);

        assertEquals(400, response.getStatusCodeValue());
    }

    @Test
    void validarPrograma_noEncontrado_devuelve404() {
        when(programaService.findById(9L)).thenReturn(Optional.empty());

        ResponseEntity<?> response = validationController.validarPrograma(9L);

        assertEquals(404, response.getStatusCodeValue());
    }

    @Test
    void validarPrograma_ok_devuelve200() {
        Programa p = new Programa();
        when(programaService.findById(1L)).thenReturn(Optional.of(p));
        when(validationService.validarIntegridadPrograma(p)).thenReturn(valido());

        ResponseEntity<?> response = validationController.validarPrograma(1L);

        assertEquals(200, response.getStatusCodeValue());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("Programa", body.get("entidad"));
    }

    @Test
    void validarPrograma_excepcion_devuelve400() {
        when(programaService.findById(1L)).thenThrow(new RuntimeException("fail"));

        ResponseEntity<?> response = validationController.validarPrograma(1L);

        assertEquals(400, response.getStatusCodeValue());
    }

    @Test
    void validarSistemaCompleto_okValido_devuelve200() {
        when(validationService.validarSistemaCompleto()).thenReturn(valido());

        ResponseEntity<?> response = validationController.validarSistemaCompleto();

        assertEquals(200, response.getStatusCodeValue());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals(true, body.get("valida"));
        assertTrue(body.get("mensaje").toString().contains("sin errores"));
    }

    @Test
    void validarSistemaCompleto_conErrores_devuelve200() {
        when(validationService.validarSistemaCompleto()).thenReturn(invalido());

        ResponseEntity<?> response = validationController.validarSistemaCompleto();

        assertEquals(200, response.getStatusCodeValue());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals(false, body.get("valida"));
        assertTrue(body.get("mensaje").toString().contains("errores críticos"));
    }

    @Test
    void validarSistemaCompleto_excepcion_devuelve400() {
        when(validationService.validarSistemaCompleto()).thenThrow(new RuntimeException("fail"));

        ResponseEntity<?> response = validationController.validarSistemaCompleto();

        assertEquals(400, response.getStatusCodeValue());
    }

    @Test
    void obtenerResumenValidacion_saludable_devuelve200() {
        when(validationService.validarSistemaCompleto()).thenReturn(valido());

        ResponseEntity<?> response = validationController.obtenerResumenValidacion();

        assertEquals(200, response.getStatusCodeValue());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("SALUDABLE", body.get("estado_general"));
    }

    @Test
    void obtenerResumenValidacion_requiereAtencion_devuelve200() {
        when(validationService.validarSistemaCompleto()).thenReturn(invalido());

        ResponseEntity<?> response = validationController.obtenerResumenValidacion();

        assertEquals(200, response.getStatusCodeValue());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("REQUIERE_ATENCION", body.get("estado_general"));
    }

    @Test
    void obtenerResumenValidacion_excepcion_devuelve400() {
        when(validationService.validarSistemaCompleto()).thenThrow(new RuntimeException("fail"));

        ResponseEntity<?> response = validationController.obtenerResumenValidacion();

        assertEquals(400, response.getStatusCodeValue());
    }
}
