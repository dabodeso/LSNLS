package com.lsnls.service;

import com.lsnls.dto.ProgramaRepetidoDTO;
import com.lsnls.entity.Concursante;
import com.lsnls.entity.Programa;
import com.lsnls.entity.ProgramaRepetido;
import com.lsnls.entity.ProgramaRepetidoConcursante;
import com.lsnls.repository.ConcursanteRepository;
import com.lsnls.repository.ProgramaRepetidoConcursanteRepository;
import com.lsnls.repository.ProgramaRepetidoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProgramaRepetidoServiceUpdateCampoTest {

    @Mock
    private ProgramaRepetidoRepository programaRepetidoRepository;

    @Mock
    private ProgramaRepetidoConcursanteRepository enlaceRepository;

    @Mock
    private ConcursanteRepository concursanteRepository;

    @InjectMocks
    private ProgramaRepetidoService service;

    private ProgramaRepetido repetido;

    @BeforeEach
    void setUp() {
        repetido = new ProgramaRepetido();
        repetido.setId(7L);
        repetido.setTipo("REPE");
        repetido.setCodigo("3");
        repetido.setTemporada(2);
        repetido.setEstado(Programa.EstadoPrograma.borrador);
    }

    @Test
    void updateCampo_cambiaEstadoYFecha() {
        when(programaRepetidoRepository.findById(7L)).thenReturn(Optional.of(repetido));
        when(programaRepetidoRepository.findByTipoAndCodigoAndTemporadaAndFechaEmision(
                "REPE", "3", 2, LocalDate.of(2026, 11, 5))).thenReturn(Optional.empty());
        when(programaRepetidoRepository.save(any(ProgramaRepetido.class))).thenAnswer(inv -> inv.getArgument(0));

        Map<String, Object> campos = new HashMap<>();
        campos.put("estado", "programado");
        campos.put("fechaEmision", "2026-11-05");
        campos.put("notas", "Reposición de noviembre");

        ProgramaRepetidoDTO dto = service.updateCampo(7L, campos);

        assertEquals("programado", dto.getEstado());
        assertEquals(LocalDate.of(2026, 11, 5), dto.getFechaEmision());
        assertEquals("Reposición de noviembre", dto.getNotas());
        assertEquals(Boolean.TRUE, dto.isRepetido());
    }

    @Test
    void updateCampo_puedePasarAEmitidoSiAunNoLoEsta() {
        when(programaRepetidoRepository.findById(7L)).thenReturn(Optional.of(repetido));
        when(programaRepetidoRepository.findByTipoAndCodigoAndTemporadaAndFechaEmisionIsNull("REPE", "3", 2))
                .thenReturn(Optional.of(repetido));
        when(programaRepetidoRepository.save(any(ProgramaRepetido.class))).thenAnswer(inv -> inv.getArgument(0));

        ProgramaRepetidoDTO dto = service.updateCampo(7L, Map.of("estado", "emitido"));

        assertEquals("emitido", dto.getEstado());
    }

    @Test
    void updateCampo_emitidoRechazaCualquierCambio() {
        repetido.setEstado(Programa.EstadoPrograma.emitido);
        when(programaRepetidoRepository.findById(7L)).thenReturn(Optional.of(repetido));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.updateCampo(7L, Map.of("estado", "borrador")));

        assertEquals("Un programa emitido no se puede editar.", error.getMessage());
        verify(programaRepetidoRepository, never()).save(any());
    }

    @Test
    void updateCampo_estadoInvalido() {
        when(programaRepetidoRepository.findById(7L)).thenReturn(Optional.of(repetido));

        assertThrows(IllegalArgumentException.class, () -> service.updateCampo(7L, Map.of("estado", "archivado")));
        verify(programaRepetidoRepository, never()).save(any());
    }

    @Test
    void updateCampo_campoNoEditable() {
        assertThrows(IllegalArgumentException.class,
                () -> service.updateCampo(7L, Map.of("duracionObjetivo", "50m")));
        verify(programaRepetidoRepository, never()).findById(any());
    }

    @Test
    void updateCampo_vacio() {
        assertThrows(IllegalArgumentException.class, () -> service.updateCampo(7L, new HashMap<>()));
    }

    @Test
    void updateCampo_codigoDuplicado() {
        when(programaRepetidoRepository.findById(7L)).thenReturn(Optional.of(repetido));
        ProgramaRepetido otro = new ProgramaRepetido();
        otro.setId(8L);
        when(programaRepetidoRepository.findByTipoAndCodigoAndTemporadaAndFechaEmisionIsNull("REPE", "9", 2))
                .thenReturn(Optional.of(otro));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.updateCampo(7L, Map.of("codigo", "9")));

        assertEquals(true, error.getMessage().contains("Ya existe un repetido"));
        verify(programaRepetidoRepository, never()).save(any());
    }

    @Test
    void updateCampo_fechaVaciaLaBorra() {
        repetido.setFechaEmision(LocalDate.of(2026, 1, 1));
        when(programaRepetidoRepository.findById(7L)).thenReturn(Optional.of(repetido));
        when(programaRepetidoRepository.findByTipoAndCodigoAndTemporadaAndFechaEmisionIsNull("REPE", "3", 2))
                .thenReturn(Optional.empty());
        when(programaRepetidoRepository.save(any(ProgramaRepetido.class))).thenAnswer(inv -> inv.getArgument(0));

        ProgramaRepetidoDTO dto = service.updateCampo(7L, Map.of("fechaEmision", ""));

        assertNull(dto.getFechaEmision());
    }

    @Test
    void updateCampo_noEncontrado() {
        when(programaRepetidoRepository.findById(7L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.updateCampo(7L, Map.of("notas", "x")));
    }

    @Test
    void updateCampo_fechaEnEditadoPasaAProgramado() {
        repetido.setEstado(Programa.EstadoPrograma.editado);
        when(programaRepetidoRepository.findById(7L)).thenReturn(Optional.of(repetido));
        when(programaRepetidoRepository.findByTipoAndCodigoAndTemporadaAndFechaEmision(
                "REPE", "3", 2, LocalDate.of(2026, 11, 5))).thenReturn(Optional.empty());
        when(programaRepetidoRepository.save(any(ProgramaRepetido.class))).thenAnswer(inv -> inv.getArgument(0));

        ProgramaRepetidoDTO dto = service.updateCampo(7L, Map.of("fechaEmision", "2026-11-05"));

        assertEquals("programado", dto.getEstado());
        assertEquals(LocalDate.of(2026, 11, 5), dto.getFechaEmision());
    }

    @Test
    void updateCampo_quitarFechaEnEditadoConservaElEstado() {
        repetido.setEstado(Programa.EstadoPrograma.editado);
        repetido.setFechaEmision(LocalDate.of(2026, 1, 1));
        when(programaRepetidoRepository.findById(7L)).thenReturn(Optional.of(repetido));
        when(programaRepetidoRepository.findByTipoAndCodigoAndTemporadaAndFechaEmisionIsNull("REPE", "3", 2))
                .thenReturn(Optional.empty());
        when(programaRepetidoRepository.save(any(ProgramaRepetido.class))).thenAnswer(inv -> inv.getArgument(0));

        ProgramaRepetidoDTO dto = service.updateCampo(7L, Map.of("fechaEmision", ""));

        assertNull(dto.getFechaEmision());
        assertEquals("editado", dto.getEstado());
    }

    @Test
    void updateCampo_quitarFechaEnProgramadoNoLoBaja() {
        repetido.setEstado(Programa.EstadoPrograma.programado);
        repetido.setFechaEmision(LocalDate.of(2026, 1, 1));
        when(programaRepetidoRepository.findById(7L)).thenReturn(Optional.of(repetido));
        when(programaRepetidoRepository.findByTipoAndCodigoAndTemporadaAndFechaEmisionIsNull("REPE", "3", 2))
                .thenReturn(Optional.empty());
        when(programaRepetidoRepository.save(any(ProgramaRepetido.class))).thenAnswer(inv -> inv.getArgument(0));

        ProgramaRepetidoDTO dto = service.updateCampo(7L, Map.of("fechaEmision", ""));

        assertNull(dto.getFechaEmision());
        assertEquals("programado", dto.getEstado());
    }

    @Test
    void updateCampo_fechaEnBorradorNoCambiaElEstado() {
        when(programaRepetidoRepository.findById(7L)).thenReturn(Optional.of(repetido));
        when(programaRepetidoRepository.findByTipoAndCodigoAndTemporadaAndFechaEmision(
                "REPE", "3", 2, LocalDate.of(2026, 11, 5))).thenReturn(Optional.empty());
        when(programaRepetidoRepository.save(any(ProgramaRepetido.class))).thenAnswer(inv -> inv.getArgument(0));

        ProgramaRepetidoDTO dto = service.updateCampo(7L, Map.of("fechaEmision", "2026-11-05"));

        assertEquals("borrador", dto.getEstado());
        assertEquals(LocalDate.of(2026, 11, 5), dto.getFechaEmision());
    }

    @Test
    void updateCampo_editadoRechazaConcursanteNoEditado() {
        when(programaRepetidoRepository.findById(7L)).thenReturn(Optional.of(repetido));
        ProgramaRepetidoConcursante enlace = new ProgramaRepetidoConcursante();
        enlace.setConcursanteId(10L);
        when(enlaceRepository.findByProgramaRepetidoIdOrderByOrdenEscaletaAsc(7L))
                .thenReturn(Collections.singletonList(enlace));
        Concursante concursante = new Concursante();
        concursante.setId(10L);
        concursante.setNumeroConcursante(42);
        concursante.setEstado("grabado");
        when(concursanteRepository.findById(10L)).thenReturn(Optional.of(concursante));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.updateCampo(7L, Map.of("estado", "editado")));

        assertEquals("No se puede pasar el programa a editado: el concursante 42 no está editado.", error.getMessage());
        assertTrue(error.getMessage().contains("42"));
        assertEquals(Programa.EstadoPrograma.borrador, repetido.getEstado());
        verify(programaRepetidoRepository, never()).save(any());
    }

    @Test
    void updateCampo_fechaYEstadoExplicitoRespetaElEstado() {
        repetido.setEstado(Programa.EstadoPrograma.editado);
        when(programaRepetidoRepository.findById(7L)).thenReturn(Optional.of(repetido));
        when(programaRepetidoRepository.findByTipoAndCodigoAndTemporadaAndFechaEmision(
                "REPE", "3", 2, LocalDate.of(2026, 11, 5))).thenReturn(Optional.empty());
        when(programaRepetidoRepository.save(any(ProgramaRepetido.class))).thenAnswer(inv -> inv.getArgument(0));

        Map<String, Object> campos = new HashMap<>();
        campos.put("fechaEmision", "2026-11-05");
        campos.put("estado", "grabado");

        ProgramaRepetidoDTO dto = service.updateCampo(7L, campos);

        assertEquals("grabado", dto.getEstado());
        assertEquals(LocalDate.of(2026, 11, 5), dto.getFechaEmision());
    }
}
