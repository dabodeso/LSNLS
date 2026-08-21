package com.lsnls.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReciclajeComboDTOTest {

    @Test
    void constructorYGetters() {
        ReciclajeComboDTO dto = new ReciclajeComboDTO(1L, 2L, 3L, 4L);

        assertEquals(1L, dto.getJornadaId());
        assertEquals(2L, dto.getComboPadreId());
        assertEquals(3L, dto.getComboHijoId());
        assertEquals(4L, dto.getPreguntaUsadaId());
    }

    @Test
    void setters() {
        ReciclajeComboDTO dto = new ReciclajeComboDTO(0L, 0L, 0L, 0L);
        dto.setJornadaId(10L);
        dto.setComboPadreId(20L);
        dto.setComboHijoId(30L);
        dto.setPreguntaUsadaId(40L);

        assertEquals(10L, dto.getJornadaId());
        assertEquals(20L, dto.getComboPadreId());
        assertEquals(30L, dto.getComboHijoId());
        assertEquals(40L, dto.getPreguntaUsadaId());
    }
}
