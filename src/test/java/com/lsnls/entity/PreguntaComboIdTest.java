package com.lsnls.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PreguntaComboIdTest {

    @Test
    void equalsYHashCode() {
        PreguntaCombo.PreguntaComboId a = new PreguntaCombo.PreguntaComboId();
        a.setPreguntaId(1L);
        a.setComboId(2L);
        PreguntaCombo.PreguntaComboId b = new PreguntaCombo.PreguntaComboId();
        b.setPreguntaId(1L);
        b.setComboId(2L);
        PreguntaCombo.PreguntaComboId c = new PreguntaCombo.PreguntaComboId();
        c.setPreguntaId(9L);
        c.setComboId(2L);

        assertEquals(a, a);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, c);
        assertFalse(a.equals(null));
        assertFalse(a.equals("x"));

        PreguntaCombo pc1 = new PreguntaCombo();
        pc1.setId(a);
        PreguntaCombo pc2 = new PreguntaCombo();
        pc2.setId(b);
        assertEquals(pc1, pc2);
        assertEquals(pc1.hashCode(), pc2.hashCode());
        assertTrue(pc1.equals(pc1));
        assertFalse(pc1.equals(null));
    }
}
