package com.lsnls.service;

import com.lsnls.entity.Concursante;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProgramaRepetidoServiceDuracionFiltroTest {

    @Test
    void margenDeVeinteMinutosEsInclusivo() {
        int objetivo = ProgramaRepetidoService.segundosFiltroTiempo("20:00");
        assertEquals(20 * 60, objetivo);
        assertTrue(ProgramaRepetidoService.duracionDentroDeMargen("19:30", objetivo));
        assertTrue(ProgramaRepetidoService.duracionDentroDeMargen("20:00", objetivo));
        assertTrue(ProgramaRepetidoService.duracionDentroDeMargen("20:30", objetivo));
        assertFalse(ProgramaRepetidoService.duracionDentroDeMargen("19:29", objetivo));
        assertFalse(ProgramaRepetidoService.duracionDentroDeMargen("20:31", objetivo));
    }

    @Test
    void tiempoVacioNoFiltraYElFormatoInvalidoFalla() {
        assertNull(ProgramaRepetidoService.segundosFiltroTiempo(null));
        assertNull(ProgramaRepetidoService.segundosFiltroTiempo(""));
        assertNull(ProgramaRepetidoService.segundosFiltroTiempo("   "));
        assertThrows(IllegalArgumentException.class, () -> ProgramaRepetidoService.segundosFiltroTiempo("abc"));
        assertThrows(IllegalArgumentException.class, () -> ProgramaRepetidoService.segundosFiltroTiempo("20:60"));
        assertThrows(IllegalArgumentException.class, () -> ProgramaRepetidoService.segundosFiltroTiempo("20"));
    }

    @Test
    void laDuracionPrioritariaEsFinalLuegoDireccionLuegoGrabacion() {
        Concursante concursante = new Concursante();
        concursante.setDuracion("05:00");
        concursante.setDuracionDireccion("10:00");
        concursante.setDuracionFinal("20:00");
        assertEquals("20:00", ProgramaRepetidoService.duracionPrioritaria(concursante));

        concursante.setDuracionFinal("  ");
        assertEquals("10:00", ProgramaRepetidoService.duracionPrioritaria(concursante));

        concursante.setDuracionDireccion("");
        assertEquals("05:00", ProgramaRepetidoService.duracionPrioritaria(concursante));

        int objetivo = ProgramaRepetidoService.segundosFiltroTiempo("20:00");
        assertFalse(ProgramaRepetidoService.duracionDentroDeMargen(
                ProgramaRepetidoService.duracionPrioritaria(concursante), objetivo));
    }
}
