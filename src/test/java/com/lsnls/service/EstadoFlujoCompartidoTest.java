package com.lsnls.service;

import com.lsnls.entity.Combo;
import com.lsnls.entity.Combo.EstadoCombo;
import com.lsnls.entity.Cuestionario;
import com.lsnls.entity.Cuestionario.EstadoCuestionario;
import com.lsnls.entity.Pregunta;
import com.lsnls.entity.PreguntaCombo;
import com.lsnls.entity.PreguntaCuestionario;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * El grafo de estados de combo y cuestionario debe coincidir
 * (salvo reaprovechado/liberado, que solo existen en combo).
 */
class EstadoFlujoCompartidoTest {

    private final ComboService comboService = new ComboService();
    private final CuestionarioService cuestionarioService = new CuestionarioService();

    @Test
    void noAdmin_mismoGrafoHastaAprobado() {
        assertDoesNotThrow(() -> cuestionarioService.validarTransicionEstado(
            EstadoCuestionario.borrador, EstadoCuestionario.revisar, false));
        assertDoesNotThrow(() -> comboService.validarTransicionEstado(
            EstadoCombo.borrador, EstadoCombo.revisar, false));

        assertDoesNotThrow(() -> cuestionarioService.validarTransicionEstado(
            EstadoCuestionario.revisar, EstadoCuestionario.aprobado, false));
        assertDoesNotThrow(() -> comboService.validarTransicionEstado(
            EstadoCombo.revisar, EstadoCombo.aprobado, false));

        assertDoesNotThrow(() -> cuestionarioService.validarTransicionEstado(
            EstadoCuestionario.revisar, EstadoCuestionario.corregir, false));
        assertDoesNotThrow(() -> comboService.validarTransicionEstado(
            EstadoCombo.revisar, EstadoCombo.corregir, false));
    }

    @Test
    void noAdmin_noSaltaDeBorradorAAprobadoNiAGrabado() {
        IllegalArgumentException cuestAprobado = assertThrows(IllegalArgumentException.class,
            () -> cuestionarioService.validarTransicionEstado(
                EstadoCuestionario.borrador, EstadoCuestionario.aprobado, false));
        IllegalArgumentException comboAprobado = assertThrows(IllegalArgumentException.class,
            () -> comboService.validarTransicionEstado(
                EstadoCombo.borrador, EstadoCombo.aprobado, false));
        assertTrue(cuestAprobado.getMessage().contains("no permitida"));
        assertTrue(comboAprobado.getMessage().contains("no permitida"));

        assertThrows(IllegalArgumentException.class, () -> cuestionarioService.validarTransicionEstado(
            EstadoCuestionario.borrador, EstadoCuestionario.grabado, false));
        assertThrows(IllegalArgumentException.class, () -> comboService.validarTransicionEstado(
            EstadoCombo.borrador, EstadoCombo.grabado, false));
    }

    @Test
    void noAdmin_aprobadoAdjudicadoYGrabadoSonFinales() {
        assertThrows(IllegalArgumentException.class, () -> cuestionarioService.validarTransicionEstado(
            EstadoCuestionario.aprobado, EstadoCuestionario.grabado, false));
        assertThrows(IllegalArgumentException.class, () -> comboService.validarTransicionEstado(
            EstadoCombo.aprobado, EstadoCombo.grabado, false));
        assertThrows(IllegalArgumentException.class, () -> cuestionarioService.validarTransicionEstado(
            EstadoCuestionario.adjudicado, EstadoCuestionario.aprobado, false));
        assertThrows(IllegalArgumentException.class, () -> comboService.validarTransicionEstado(
            EstadoCombo.adjudicado, EstadoCombo.aprobado, false));
        assertThrows(IllegalArgumentException.class, () -> comboService.validarTransicionEstado(
            EstadoCombo.reaprovechado, EstadoCombo.aprobado, false));
        assertThrows(IllegalArgumentException.class, () -> comboService.validarTransicionEstado(
            EstadoCombo.liberado, EstadoCombo.aprobado, false));
    }

    @Test
    void admin_puedeSaltarElGrafo() {
        assertDoesNotThrow(() -> cuestionarioService.validarTransicionEstado(
            EstadoCuestionario.borrador, EstadoCuestionario.grabado, true));
        assertDoesNotThrow(() -> comboService.validarTransicionEstado(
            EstadoCombo.borrador, EstadoCombo.grabado, true));
        assertDoesNotThrow(() -> comboService.validarTransicionEstado(
            EstadoCombo.corregir, EstadoCombo.aprobado, true));
    }

    @Test
    void noAdmin_cuestionarioIncompletoNoSaleDeBorrador() {
        Cuestionario c = new Cuestionario();
        c.setPreguntas(new HashSet<>());
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> cuestionarioService.validarCompletoParaAprobar(c));
        assertTrue(ex.getMessage().contains("exactamente 4"));
    }

    @Test
    void noAdmin_comboIncompletoNoSaleDeBorrador() {
        Combo c = new Combo();
        c.setPreguntas(new HashSet<>());
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> comboService.validarCompletoParaAprobar(c));
        assertTrue(ex.getMessage().contains("exactamente 3"));
    }

    @Test
    void cuestionarioCompleto_cuatroNiveles() {
        Cuestionario c = new Cuestionario();
        Set<PreguntaCuestionario> slots = new HashSet<>();
        slots.add(slotCuest(Pregunta.NivelPregunta._1LS));
        slots.add(slotCuest(Pregunta.NivelPregunta._2NLS));
        slots.add(slotCuest(Pregunta.NivelPregunta._3LS));
        slots.add(slotCuest(Pregunta.NivelPregunta._4NLS));
        c.setPreguntas(slots);
        assertDoesNotThrow(() -> cuestionarioService.validarCompletoParaAprobar(c));
    }

    @Test
    void comboCompleto_tresFactores() {
        Combo c = new Combo();
        Set<PreguntaCombo> slots = new HashSet<>();
        slots.add(slotCombo(1, "X2"));
        slots.add(slotCombo(2, "X3"));
        slots.add(slotCombo(3, "X"));
        c.setPreguntas(slots);
        assertDoesNotThrow(() -> comboService.validarCompletoParaAprobar(c));
    }

    private PreguntaCuestionario slotCuest(Pregunta.NivelPregunta nivel) {
        Pregunta p = new Pregunta();
        p.setNivel(nivel);
        PreguntaCuestionario pc = new PreguntaCuestionario();
        pc.setPregunta(p);
        return pc;
    }

    private PreguntaCombo slotCombo(int posicion, String factor) {
        Pregunta p = new Pregunta();
        p.setNivel(Pregunta.NivelPregunta._5LS);
        PreguntaCombo pc = new PreguntaCombo();
        pc.setPregunta(p);
        pc.setPosicion(posicion);
        pc.setFactorMultiplicacion(factor);
        return pc;
    }
}
