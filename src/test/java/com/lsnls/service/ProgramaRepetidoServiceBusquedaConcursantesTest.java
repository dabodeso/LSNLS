package com.lsnls.service;

import com.lsnls.dto.ConcursanteEmitidoOpcionDTO;
import com.lsnls.dto.ResultadoBusquedaDTO;
import com.lsnls.entity.Concursante;
import com.lsnls.entity.Programa;
import com.lsnls.entity.ProgramaConcursante;
import com.lsnls.entity.ProgramaRepetidoConcursante;
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
class ProgramaRepetidoServiceBusquedaConcursantesTest {

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
    void cientoTreintaEmitidos() {
        Programa programa = new Programa();
        programa.setId(1L);
        programa.setCodigo("10");
        programa.setTemporada(1);
        programa.setEstado(Programa.EstadoPrograma.emitido);

        List<Concursante> concursantes = new ArrayList<>();
        for (int i = 1; i <= 130; i++) {
            Concursante concursante = new Concursante();
            concursante.setId((long) i);
            concursante.setNumeroConcursante(i);
            concursante.setNumeroPrograma(1);
            concursante.setOrdenEscaleta(i);
            concursante.setEstado("editado");
            concursante.setNombre(i <= 40 ? "Ana " + i : "Luis " + i);
            concursante.setLugar(i == 130 ? "Madrid" : "Bilbao");
            concursante.setValoracionFinal(i <= 40 ? "3" : "1");
            concursante.setDuracionFinal(i <= 10 ? "02:00" : "01:00");
            concursantes.add(concursante);
        }

        when(programaRepository.findByEstado(Programa.EstadoPrograma.emitido)).thenReturn(List.of(programa));
        when(programaConcursanteRepository.findByProgramaIdIn(any())).thenReturn(List.of());
        when(concursanteRepository.findByNumeroProgramaIn(any())).thenReturn(concursantes);
        when(concursanteRepository.findAllById(any())).thenReturn(concursantes);
    }

    @Test
    void laPaginaQueSuperaElAntiguoTopeDe120TraeElTotal() {
        ResultadoBusquedaDTO<ConcursanteEmitidoOpcionDTO> pagina =
                service.buscarConcursantesEmitidos(null, null, null, null, null, 4, 25);

        assertEquals(130L, pagina.getTotal());
        assertEquals(4, pagina.getPage());
        assertEquals(25, pagina.getSize());
        assertEquals(25, pagina.getResultados().size());
        assertEquals(101, pagina.getResultados().get(0).getNumeroConcursante());
        assertEquals(125, pagina.getResultados().get(24).getNumeroConcursante());
        assertTrue(pagina.isHayMas());
    }

    @Test
    void laUltimaPaginaIncluyeQuienesQuedabanFueraDelTope() {
        ResultadoBusquedaDTO<ConcursanteEmitidoOpcionDTO> pagina =
                service.buscarConcursantesEmitidos(null, null, null, null, null, 5, 25);

        assertEquals(130L, pagina.getTotal());
        assertEquals(5, pagina.getPage());
        assertEquals(5, pagina.getResultados().size());
        assertEquals(126, pagina.getResultados().get(0).getNumeroConcursante());
        assertEquals(130, pagina.getResultados().get(4).getNumeroConcursante());
        assertFalse(pagina.isHayMas());
    }

    @Test
    void laSegundaPaginaEmpiezaEnEl26() {
        ResultadoBusquedaDTO<ConcursanteEmitidoOpcionDTO> pagina =
                service.buscarConcursantesEmitidos(null, null, null, null, null, 1, 25);

        assertEquals(130L, pagina.getTotal());
        assertEquals(1, pagina.getPage());
        assertEquals(26, pagina.getResultados().get(0).getNumeroConcursante());
        assertEquals(50, pagina.getResultados().get(24).getNumeroConcursante());
        assertTrue(pagina.isHayMas());
    }

    @Test
    void laSegundaPaginaConservaElFiltroDeRepetido() {
        List<ProgramaConcursante> extra = new ArrayList<>();
        for (long id = 1; id <= 40; id++) {
            extra.add(enlaceNormal(2L, id));
        }
        when(programaConcursanteRepository.findByConcursanteIdIn(any())).thenReturn(extra);
        when(enlaceRepository.findByConcursanteIdIn(any())).thenReturn(List.of());

        ResultadoBusquedaDTO<ConcursanteEmitidoOpcionDTO> repetidos =
                service.buscarConcursantesEmitidos(null, null, null, null, "repetido", 1, 25);

        assertEquals(40L, repetidos.getTotal());
        assertEquals(1, repetidos.getPage());
        assertEquals(15, repetidos.getResultados().size());
        assertEquals(26, repetidos.getResultados().get(0).getNumeroConcursante());
        assertEquals(40, repetidos.getResultados().get(14).getNumeroConcursante());
        assertFalse(repetidos.isHayMas());

        ResultadoBusquedaDTO<ConcursanteEmitidoOpcionDTO> emitidos =
                service.buscarConcursantesEmitidos(null, null, null, null, "emitido", 0, 25);
        assertEquals(90L, emitidos.getTotal());
        assertEquals(25, emitidos.getResultados().size());
        assertEquals(41, emitidos.getResultados().get(0).getNumeroConcursante());
        assertTrue(emitidos.isHayMas());
    }

    @Test
    void emitidoEsUnaSalidaYRepetidoSumaOtraAunqueElEstadoGuardadoDigaOtraCosa() {
        when(programaConcursanteRepository.findByConcursanteIdIn(any())).thenReturn(List.of(
                enlaceNormal(1L, 1L),
                enlaceNormal(2L, 2L)));
        when(enlaceRepository.findByConcursanteIdIn(any())).thenReturn(List.of(
                enlaceRepetido(9L, 3L),
                enlaceRepetido(9L, 3L),
                enlaceRepetido(1L, 5L),
                enlaceRepetido(9L, 4L),
                enlaceRepetido(10L, 4L)));

        ResultadoBusquedaDTO<ConcursanteEmitidoOpcionDTO> emitidos =
                service.buscarConcursantesEmitidos(null, null, null, null, "Emitido", 0, 25);
        assertEquals(126L, emitidos.getTotal());
        assertEquals(1, emitidos.getResultados().get(0).getNumeroConcursante());
        assertTrue(emitidos.getResultados().stream().noneMatch(dto ->
                dto.getNumeroConcursante() == 2
                        || dto.getNumeroConcursante() == 3
                        || dto.getNumeroConcursante() == 4
                        || dto.getNumeroConcursante() == 5));

        ResultadoBusquedaDTO<ConcursanteEmitidoOpcionDTO> repetidos =
                service.buscarConcursantesEmitidos(null, null, null, null, "repetido", 0, 25);
        assertEquals(4L, repetidos.getTotal());
        assertEquals(4, repetidos.getResultados().size());
        assertEquals(2, repetidos.getResultados().get(0).getNumeroConcursante());
        assertEquals(3, repetidos.getResultados().get(1).getNumeroConcursante());
        assertEquals(4, repetidos.getResultados().get(2).getNumeroConcursante());
        assertEquals(5, repetidos.getResultados().get(3).getNumeroConcursante());
        assertFalse(repetidos.isHayMas());

        ResultadoBusquedaDTO<ConcursanteEmitidoOpcionDTO> todos =
                service.buscarConcursantesEmitidos(null, null, null, null, "", 0, 25);
        assertEquals(130L, todos.getTotal());
    }

    @Test
    void losFiltrosAcotanElTotalAntesDePaginar() {
        ResultadoBusquedaDTO<ConcursanteEmitidoOpcionDTO> porValoracion =
                service.buscarConcursantesEmitidos("3", null, null, null, null, 0, 25);
        assertEquals(40L, porValoracion.getTotal());
        assertEquals(25, porValoracion.getResultados().size());
        assertTrue(porValoracion.isHayMas());

        ResultadoBusquedaDTO<ConcursanteEmitidoOpcionDTO> porTiempo =
                service.buscarConcursantesEmitidos(null, "02:00", null, null, null, 0, 25);
        assertEquals(10L, porTiempo.getTotal());
        assertEquals(10, porTiempo.getResultados().size());
        assertFalse(porTiempo.isHayMas());

        ResultadoBusquedaDTO<ConcursanteEmitidoOpcionDTO> porNumero =
                service.buscarConcursantesEmitidos(null, null, "7", null, null, 0, 25);
        assertEquals(1L, porNumero.getTotal());
        assertEquals(7, porNumero.getResultados().get(0).getNumeroConcursante());

        ResultadoBusquedaDTO<ConcursanteEmitidoOpcionDTO> porCiudad =
                service.buscarConcursantesEmitidos(null, null, null, "Madrid", null, 0, 25);
        assertEquals(1L, porCiudad.getTotal());
        assertEquals("Madrid", porCiudad.getResultados().get(0).getLugar());
    }

    @Test
    void sizeInvalidoUsaVeinticincoYUnSizePedidoSeRespeta() {
        ResultadoBusquedaDTO<ConcursanteEmitidoOpcionDTO> porDefecto =
                service.buscarConcursantesEmitidos(null, null, null, null, null, 0, 0);
        assertEquals(25, porDefecto.getSize());
        assertEquals(25, porDefecto.getResultados().size());
        assertEquals(130L, porDefecto.getTotal());

        ResultadoBusquedaDTO<ConcursanteEmitidoOpcionDTO> pagina =
                service.buscarConcursantesEmitidos(null, null, null, null, null, 1, 10);
        assertEquals(10, pagina.getSize());
        assertEquals(10, pagina.getResultados().size());
        assertEquals(11, pagina.getResultados().get(0).getNumeroConcursante());
        assertEquals(130L, pagina.getTotal());
        assertTrue(pagina.isHayMas());
    }

    private static ProgramaConcursante enlaceNormal(Long programaId, Long concursanteId) {
        ProgramaConcursante enlace = new ProgramaConcursante();
        enlace.setProgramaId(programaId);
        enlace.setConcursanteId(concursanteId);
        enlace.setOrdenEscaleta(1);
        return enlace;
    }

    private static ProgramaRepetidoConcursante enlaceRepetido(Long programaRepetidoId, Long concursanteId) {
        ProgramaRepetidoConcursante enlace = new ProgramaRepetidoConcursante();
        enlace.setProgramaRepetidoId(programaRepetidoId);
        enlace.setConcursanteId(concursanteId);
        enlace.setOrdenEscaleta(1);
        return enlace;
    }
}
