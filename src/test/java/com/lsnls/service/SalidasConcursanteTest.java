package com.lsnls.service;

import com.lsnls.repository.ConcursanteRepository;
import org.hibernate.hql.internal.ast.HqlParser;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SalidasConcursanteTest {

    @Test
    void unaSalidaEsEmitidoYMasDeUnaEsRepetido() {
        assertEquals("emitido", SalidasConcursante.clasificar(1));
        assertEquals("repetido", SalidasConcursante.clasificar(2));
        assertEquals("repetido", SalidasConcursante.clasificar(5));
        assertNull(SalidasConcursante.clasificar(0));
    }

    @Test
    void elMismoProgramaNormalCuentaUnaSolaVez() {
        assertEquals(1, SalidasConcursante.contar(5, List.of(5L), List.of()));
        assertEquals(1, SalidasConcursante.contar(5, null, null));
        assertEquals(1, SalidasConcursante.contar(null, List.of(5L, 5L), List.of()));
        assertEquals("emitido", SalidasConcursante.clasificar(
                SalidasConcursante.contar(5, List.of(5L), List.of())));
    }

    @Test
    void otroProgramaOUnRepetidoCuentaComoOtraSalida() {
        assertEquals(2, SalidasConcursante.contar(5, List.of(8L), List.of()));
        assertEquals(2, SalidasConcursante.contar(5, List.of(5L), List.of(9L)));
        // Un repetido con el mismo número de id no es el programa normal.
        assertEquals(2, SalidasConcursante.contar(5, List.of(), List.of(5L)));
        assertEquals(3, SalidasConcursante.contar(null, List.of(1L, 2L), List.of(3L, 3L)));
        assertEquals(1, SalidasConcursante.contar(null, List.of(), List.of(7L)));
        assertEquals(0, SalidasConcursante.contar(null, null, null));
        assertEquals("repetido", SalidasConcursante.clasificar(
                SalidasConcursante.contar(null, List.of(1L), List.of(2L))));
    }

    @Test
    void emitidoYRepetidoNoFiltranPorLaColumnaEstado() {
        SalidasConcursante.Filtro emitido = SalidasConcursante.interpretar("EMITIDO");
        assertNull(emitido.estado);
        assertEquals(SalidasConcursante.UNA, emitido.salidas);

        SalidasConcursante.Filtro repetido = SalidasConcursante.interpretar("Repetido");
        assertNull(repetido.estado);
        assertEquals(SalidasConcursante.MAS, repetido.salidas);

        SalidasConcursante.Filtro editado = SalidasConcursante.interpretar("editado");
        assertEquals("editado", editado.estado);
        assertNull(editado.salidas);

        SalidasConcursante.Filtro grabado = SalidasConcursante.interpretar("GRABADO");
        assertEquals("grabado", grabado.estado);
        assertNull(grabado.salidas);
    }

    @Test
    void laConsultaCuentaUnaSalidaYMasDeUna() throws Exception {
        parsear("SELECT c FROM Concursante c WHERE " + ConcursanteRepository.CUENTA_SALIDAS + " = 1");
        parsear("SELECT c FROM Concursante c WHERE " + ConcursanteRepository.CUENTA_SALIDAS + " > 1");
        String filtro = ConcursanteRepository.FILTRO_ESTADO_O_SALIDAS;
        assertTrue(filtro.contains(":salidas = '" + SalidasConcursante.UNA + "'"));
        assertTrue(filtro.contains(":salidas = '" + SalidasConcursante.MAS + "'"));
        assertTrue(filtro.contains("= 1"));
        assertTrue(filtro.contains("> 1"));
        assertTrue(filtro.contains("LOWER(c.estado) = LOWER(:estado)"));
    }

    private static void parsear(String hql) throws Exception {
        HqlParser parser = HqlParser.getInstance(hql);
        parser.statement();
    }
}
