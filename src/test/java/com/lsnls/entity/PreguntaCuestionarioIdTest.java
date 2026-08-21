package com.lsnls.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class PreguntaCuestionarioIdTest {

    @Test
    void equalsYHashCode() {
        PreguntaCuestionario.PreguntaCuestionarioId a = new PreguntaCuestionario.PreguntaCuestionarioId();
        a.setPreguntaId(1L);
        a.setCuestionarioId(2L);
        PreguntaCuestionario.PreguntaCuestionarioId b = new PreguntaCuestionario.PreguntaCuestionarioId();
        b.setPreguntaId(1L);
        b.setCuestionarioId(2L);
        PreguntaCuestionario.PreguntaCuestionarioId c = new PreguntaCuestionario.PreguntaCuestionarioId();
        c.setPreguntaId(3L);
        c.setCuestionarioId(2L);

        assertEquals(a, a);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, c);
        assertFalse(a.equals(null));
        assertFalse(a.equals("x"));

        PreguntaCuestionario pc1 = new PreguntaCuestionario();
        pc1.setId(a);
        PreguntaCuestionario pc2 = new PreguntaCuestionario();
        pc2.setId(b);
        assertEquals(pc1, pc2);
        assertEquals(pc1.hashCode(), pc2.hashCode());
    }
}
