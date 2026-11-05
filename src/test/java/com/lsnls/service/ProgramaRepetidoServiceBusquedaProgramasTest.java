package com.lsnls.service;

import com.lsnls.dto.ProgramaEmitidoOpcionDTO;
import com.lsnls.dto.ResultadoBusquedaDTO;
import com.lsnls.entity.Programa;
import com.lsnls.repository.ConcursanteRepository;
import com.lsnls.repository.ProgramaConcursanteRepository;
import com.lsnls.repository.ProgramaRepetidoConcursanteRepository;
import com.lsnls.repository.ProgramaRepetidoRepository;
import com.lsnls.repository.ProgramaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProgramaRepetidoServiceBusquedaProgramasTest {

    @Mock
    private ProgramaRepetidoRepository programaRepetidoRepository;

    @Mock
    private ProgramaRepetidoConcursanteRepository enlaceRepository;

    @Mock
    private ConcursanteRepository concursanteRepository;

    @Mock
    private ConcursanteService concursanteService;

    @Mock
    private ProgramaRepository programaRepository;

    @Mock
    private ProgramaConcursanteRepository programaConcursanteRepository;

    @Mock
    private UndoService undoService;

    @InjectMocks
    private ProgramaRepetidoService service;

    @BeforeEach
    void emitidosSinConcursantes() {
        List<Programa> programas = new ArrayList<>();
        for (int i = 1; i <= 30; i++) {
            Programa programa = new Programa();
            programa.setId((long) i);
            programa.setCodigo(String.valueOf(i));
            programa.setTemporada(1);
            programa.setEstado(Programa.EstadoPrograma.emitido);
            programas.add(programa);
        }
        when(programaRepository.findByEstado(Programa.EstadoPrograma.emitido)).thenReturn(programas);
        when(programaConcursanteRepository.findByProgramaIdIn(any())).thenReturn(List.of());
        when(concursanteRepository.findByNumeroProgramaIn(any())).thenReturn(List.of());
    }

    @Test
    void laPrimeraPaginaTrae25YElTotal() {
        ResultadoBusquedaDTO<ProgramaEmitidoOpcionDTO> pagina =
                service.buscarProgramasEmitidos(null, null, null, 0);

        assertEquals(30L, pagina.getTotal());
        assertEquals(0, pagina.getPage());
        assertEquals(25, pagina.getSize());
        assertEquals(25, pagina.getResultados().size());
        assertTrue(pagina.isHayMas());
        assertEquals("1", pagina.getResultados().get(0).getCodigo());
        assertEquals("25", pagina.getResultados().get(24).getCodigo());
    }

    @Test
    void laSegundaPaginaTraeElResto() {
        ResultadoBusquedaDTO<ProgramaEmitidoOpcionDTO> pagina =
                service.buscarProgramasEmitidos(null, null, null, 1);

        assertEquals(30L, pagina.getTotal());
        assertEquals(1, pagina.getPage());
        assertEquals(5, pagina.getResultados().size());
        assertFalse(pagina.isHayMas());
        assertEquals("26", pagina.getResultados().get(0).getCodigo());
        assertEquals("30", pagina.getResultados().get(4).getCodigo());
    }

    @Test
    void unaPaginaFueraDeRangoSeQuedaEnLaUltima() {
        ResultadoBusquedaDTO<ProgramaEmitidoOpcionDTO> pagina =
                service.buscarProgramasEmitidos(null, null, null, 9);

        assertEquals(1, pagina.getPage());
        assertEquals("26", pagina.getResultados().get(0).getCodigo());
    }

    @Test
    void elFiltroDeCodigoSeMantieneAlPaginar() {
        ResultadoBusquedaDTO<ProgramaEmitidoOpcionDTO> pagina =
                service.buscarProgramasEmitidos(null, "12", null, 0);

        assertEquals(1L, pagina.getTotal());
        assertEquals(0, pagina.getPage());
        assertFalse(pagina.isHayMas());
        assertEquals("12", pagina.getResultados().get(0).getCodigo());
    }
}
