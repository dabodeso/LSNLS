package com.lsnls.dto;

import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CrearCuestionarioDTOTest {

    @Test
    void gettersSettersYSlots() {
        CrearCuestionarioDTO dto = new CrearCuestionarioDTO();
        dto.setVersion(1L);
        dto.setPreguntasNormales(Collections.singletonList(10L));
        dto.setTematica("CINE");
        dto.setNotasDireccion("ok");
        CrearCuestionarioDTO.PreguntaMultiplicadoraDTO pm = new CrearCuestionarioDTO.PreguntaMultiplicadoraDTO();
        pm.setId(5L);
        pm.setFactor("X2");
        dto.setPreguntasMultiplicadoras(Collections.singletonList(pm));

        assertEquals(1L, dto.getVersion());
        assertEquals(10L, dto.getPreguntasNormales().get(0));
        assertEquals("CINE", dto.getTematica());
        assertEquals("ok", dto.getNotasDireccion());
        assertEquals(5L, dto.getPreguntasMultiplicadoras().get(0).getId());
        assertEquals("X2", dto.getPreguntasMultiplicadoras().get(0).getFactor());

        assertEquals("1LS", CrearCuestionarioDTO.getSlotPorIndice(0, false));
        assertEquals("2NLS", CrearCuestionarioDTO.getSlotPorIndice(1, false));
        assertEquals("3LS", CrearCuestionarioDTO.getSlotPorIndice(2, false));
        assertEquals("4NLS", CrearCuestionarioDTO.getSlotPorIndice(3, false));
        assertNull(CrearCuestionarioDTO.getSlotPorIndice(9, false));
        assertEquals("PM1", CrearCuestionarioDTO.getSlotPorIndice(0, true));
        assertEquals("PM2", CrearCuestionarioDTO.getSlotPorIndice(1, true));
        assertEquals("PM3", CrearCuestionarioDTO.getSlotPorIndice(2, true));
        assertNull(CrearCuestionarioDTO.getSlotPorIndice(4, true));
    }
}
