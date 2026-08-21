package com.lsnls.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EstadoConcursanteTest {

    @Test
    void valoresEnum() {
        assertEquals(5, EstadoConcursante.values().length);
        assertEquals(EstadoConcursante.EDITADO, EstadoConcursante.valueOf("EDITADO"));
        assertEquals("GRABADO", EstadoConcursante.GRABADO.name());
        assertEquals("PROGRAMADO", EstadoConcursante.PROGRAMADO.name());
        assertEquals("EMITIDO", EstadoConcursante.EMITIDO.name());
        assertEquals("ARCHIVADO", EstadoConcursante.ARCHIVADO.name());
    }
}
