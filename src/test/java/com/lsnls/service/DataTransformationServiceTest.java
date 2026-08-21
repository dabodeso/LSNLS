package com.lsnls.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DataTransformationServiceTest {

    private DataTransformationService service;

    @BeforeEach
    void setUp() {
        service = new DataTransformationService();
    }

    @Test
    void normalizarTexto_nullYVacio() {
        assertNull(service.normalizarTexto(null));
        assertEquals("", service.normalizarTexto(""));
        assertEquals("   ", service.normalizarTexto("   "));
    }

    @Test
    void normalizarTexto_preservaMayusculasYLimpiaEspaciosYSaltos() {
        String resultado = service.normalizarTexto("  Hola\nMundo\r\n  extra  ");
        assertEquals("Hola Mundo extra", resultado);
    }

    @Test
    void esTextoValido_casosInvalidos() {
        assertFalse(service.esTextoValido(null, 10));
        assertFalse(service.esTextoValido("", 10));
        assertFalse(service.esTextoValido("   ", 10));
        assertFalse(service.esTextoValido("demasiado largo", 5));
        assertFalse(service.esTextoValido("linea\nbreak", 100));
        assertFalse(service.esTextoValido("linea\rbreak", 100));
        assertFalse(service.esTextoValido("arroba@", 100));
    }

    @Test
    void esTextoValido_ok() {
        assertTrue(service.esTextoValido("Tema válido 1.", 100));
    }

    @Test
    void normalizarPregunta_delegaEnNormalizarTexto() {
        assertEquals("Pregunta una", service.normalizarPregunta("  Pregunta\nuna  "));
        assertNull(service.normalizarPregunta(null));
    }

    @Test
    void normalizarRespuesta_truncaA500() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 510; i++) {
            sb.append("a");
        }
        String truncada = service.normalizarRespuesta(sb.toString());
        assertEquals(500, truncada.length());
        assertNull(service.normalizarRespuesta(null));
        assertEquals("corta", service.normalizarRespuesta("  corta  "));
    }

    @Test
    void normalizarTematica_truncaA100() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 120; i++) {
            sb.append("b");
        }
        String truncada = service.normalizarTematica(sb.toString());
        assertEquals(100, truncada.length());
    }

    @Test
    void validarPreguntaCompleta_ok() {
        DataTransformationService.ValidationResult result =
                service.validarPreguntaCompleta("¿Cuál es la capital?", "Madrid", "GEOGRAFIA");
        assertTrue(result.isValid());
        assertTrue(result.getErrors().isEmpty());
        assertEquals("", result.getErrorsAsString());
    }

    @Test
    void validarPreguntaCompleta_preguntaVaciaOSalto() {
        DataTransformationService.ValidationResult vacia =
                service.validarPreguntaCompleta("  ", "ok", "TEMA");
        assertFalse(vacia.isValid());
        assertTrue(vacia.getErrors().get("pregunta").contains("vacía"));

        DataTransformationService.ValidationResult salto =
                service.validarPreguntaCompleta("a\nb", "ok", "TEMA");
        assertTrue(salto.getErrors().containsKey("pregunta"));
    }

    @Test
    void validarPreguntaCompleta_preguntaCaracteresInvalidos() {
        DataTransformationService.ValidationResult result =
                service.validarPreguntaCompleta("pregunta #rara", "ok", "TEMA");
        assertEquals("La pregunta contiene caracteres no permitidos", result.getErrors().get("pregunta"));
    }

    @Test
    void validarPreguntaCompleta_respuestaInvalida() {
        assertTrue(service.validarPreguntaCompleta("ok", null, "TEMA").getErrors().containsKey("respuesta"));
        assertTrue(service.validarPreguntaCompleta("ok", "a\nb", "TEMA").getErrors().containsKey("respuesta"));

        StringBuilder larga = new StringBuilder();
        for (int i = 0; i < 501; i++) {
            larga.append("x");
        }
        DataTransformationService.ValidationResult excesiva =
                service.validarPreguntaCompleta("ok", larga.toString(), "TEMA");
        assertTrue(excesiva.getErrors().get("respuesta").contains("500"));

        DataTransformationService.ValidationResult especial =
                service.validarPreguntaCompleta("ok", "mal@", "TEMA");
        assertEquals("La respuesta contiene caracteres no permitidos", especial.getErrors().get("respuesta"));
    }

    @Test
    void validarPreguntaCompleta_tematicaInvalida() {
        DataTransformationService.ValidationResult result =
                service.validarPreguntaCompleta("ok", "ok", "tema\nmalo");
        assertTrue(result.getErrors().containsKey("tematica"));
        assertTrue(result.getErrorsAsString().contains("temática") || result.getErrorsAsString().length() > 0);
    }

    @Test
    void validationResult_addErrorMarcaInvalido() {
        DataTransformationService.ValidationResult result = new DataTransformationService.ValidationResult();
        assertTrue(result.isValid());
        result.addError("campo", "msg1");
        result.addError("otro", "msg2");
        assertFalse(result.isValid());
        assertEquals(2, result.getErrors().size());
        assertTrue(result.getErrorsAsString().contains("msg1"));
        assertTrue(result.getErrorsAsString().contains("msg2"));
    }
}
