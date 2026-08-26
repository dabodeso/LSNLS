package com.lsnls.config;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SlotsJornadaTest {

    @Test
    void listaConHuecosConservaElIndice() {
        assertEquals(Arrays.asList(10L, null, 30L, null, null, null),
                SlotsJornada.normalizarIds(Arrays.asList(10L, null, 30L)));
    }

    @Test
    void listaCompactaLegacyLlenaDesdeElPrimero() {
        assertEquals(Arrays.asList(10L, 20L, 30L, null, null, null),
                SlotsJornada.normalizarIds(Arrays.asList(10L, 20L, 30L)));
    }

    @Test
    void vaciaSonSeisHuecos() {
        assertEquals(Arrays.asList(null, null, null, null, null, null),
                SlotsJornada.normalizarIds(Collections.emptyList()));
    }

    @Test
    void masDeSeisLanza() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> SlotsJornada.normalizarIds(Arrays.asList(1L, 2L, 3L, 4L, 5L, 6L, 7L)));
        assertTrue(ex.getMessage().contains("Máximo 6"));
    }

    @Test
    void duplicadoLanza() {
        assertThrows(IllegalArgumentException.class,
                () -> SlotsJornada.normalizarIds(Arrays.asList(1L, null, 1L)));
    }

    @Test
    void idsAsignadosIgnoraHuecos() {
        assertEquals(2, SlotsJornada.idsAsignados(Arrays.asList(10L, null, 30L, null, null, null)).size());
        assertNull(SlotsJornada.normalizarIds(Arrays.asList(10L, null, 30L)).get(1));
    }
}
