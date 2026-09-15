package com.lsnls.entity;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JornadaTest {

    @Test
    void reemplazarCuestionariosPorSlot_reutilizaLaMismaAsignacion() {
        Jornada jornada = jornada(6L);
        Cuestionario cuestionario = cuestionario(10L);
        jornada.reemplazarCuestionariosPorSlot(Arrays.asList(cuestionario, null, null, null, null, null));
        JornadaCuestionarioAsignacion original = unicaAsignacionCuestionario(jornada);

        jornada.reemplazarCuestionariosPorSlot(Arrays.asList(cuestionario, null, null, null, null, null));

        assertEquals(1, jornada.getCuestionarioAsignaciones().size());
        assertSame(original, unicaAsignacionCuestionario(jornada));
        assertEquals(1, original.getSlot());
    }

    @Test
    void reemplazarCuestionariosPorSlot_cambiaSlotSinNuevaInstancia() {
        Jornada jornada = jornada(6L);
        Cuestionario cuestionario = cuestionario(10L);
        jornada.reemplazarCuestionariosPorSlot(Arrays.asList(cuestionario, null, null, null, null, null));
        JornadaCuestionarioAsignacion original = unicaAsignacionCuestionario(jornada);

        jornada.reemplazarCuestionariosPorSlot(Arrays.asList(null, cuestionario, null, null, null, null));

        assertSame(original, unicaAsignacionCuestionario(jornada));
        assertEquals(2, original.getSlot());
    }

    @Test
    void reemplazarCuestionariosPorSlot_quitaLosQueYaNoEstan() {
        Jornada jornada = jornada(6L);
        Cuestionario seQueda = cuestionario(10L);
        Cuestionario seVa = cuestionario(11L);
        jornada.reemplazarCuestionariosPorSlot(Arrays.asList(seQueda, seVa, null, null, null, null));

        jornada.reemplazarCuestionariosPorSlot(Arrays.asList(seQueda, null, null, null, null, null));

        assertEquals(1, jornada.getCuestionarioAsignaciones().size());
        assertEquals(10L, unicaAsignacionCuestionario(jornada).getCuestionario().getId());
    }

    @Test
    void reemplazarCombosPorSlot_reutilizaLaMismaAsignacion() {
        Jornada jornada = jornada(6L);
        Combo combo = combo(4L);
        jornada.reemplazarCombosPorSlot(Arrays.asList(combo, null, null, null, null, null));
        JornadaComboAsignacion original = unicaAsignacionCombo(jornada);

        Combo otraInstanciaMismoId = combo(4L);
        jornada.reemplazarCombosPorSlot(Arrays.asList(null, otraInstanciaMismoId, null, null, null, null));

        assertEquals(1, jornada.getComboAsignaciones().size());
        assertSame(original, unicaAsignacionCombo(jornada));
        assertEquals(2, original.getSlot());
        assertEquals(4L, original.getCombo().getId());
    }

    @Test
    void reemplazarCuestionariosPorSlot_vacioLimpia() {
        Jornada jornada = jornada(6L);
        jornada.reemplazarCuestionariosPorSlot(Collections.singletonList(cuestionario(10L)));
        jornada.reemplazarCuestionariosPorSlot(null);
        assertTrue(jornada.getCuestionarioAsignaciones().isEmpty());
    }

    private static Jornada jornada(Long id) {
        Jornada jornada = new Jornada();
        jornada.setId(id);
        return jornada;
    }

    private static Cuestionario cuestionario(Long id) {
        Cuestionario c = new Cuestionario();
        c.setId(id);
        return c;
    }

    private static Combo combo(Long id) {
        Combo c = new Combo();
        c.setId(id);
        return c;
    }

    private static JornadaCuestionarioAsignacion unicaAsignacionCuestionario(Jornada jornada) {
        return jornada.getCuestionarioAsignaciones().iterator().next();
    }

    private static JornadaComboAsignacion unicaAsignacionCombo(Jornada jornada) {
        return jornada.getComboAsignaciones().iterator().next();
    }
}
