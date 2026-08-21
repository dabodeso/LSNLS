package com.lsnls.service;

import com.lsnls.dto.HistorialJornadaDTO;
import com.lsnls.dto.MarcarNoUsadoDTO;
import com.lsnls.dto.ReaprovecharComboDTO;
import com.lsnls.entity.Combo;
import com.lsnls.entity.Cuestionario;
import com.lsnls.entity.HistorialJornada;
import com.lsnls.entity.HistorialJornada.EstadoAsignacion;
import com.lsnls.entity.HistorialJornada.TipoAsignacion;
import com.lsnls.entity.Jornada;
import com.lsnls.entity.Pregunta;
import com.lsnls.entity.Usuario;
import com.lsnls.repository.ComboRepository;
import com.lsnls.repository.CuestionarioRepository;
import com.lsnls.repository.HistorialJornadaRepository;
import com.lsnls.repository.JornadaRepository;
import com.lsnls.repository.PreguntaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HistorialJornadaServiceTest {

    @Mock
    private HistorialJornadaRepository historialRepository;

    @Mock
    private CuestionarioRepository cuestionarioRepository;

    @Mock
    private ComboRepository comboRepository;

    @Mock
    private JornadaRepository jornadaRepository;

    @Mock
    private PreguntaRepository preguntaRepository;

    @Mock
    private UndoService undoService;

    @InjectMocks
    private HistorialJornadaService historialJornadaService;

    private Jornada jornada;
    private Cuestionario cuestionario;
    private Combo combo;

    @BeforeEach
    void setUp() {
        jornada = new Jornada();
        jornada.setId(1L);
        jornada.setNombre("J1");

        cuestionario = new Cuestionario();
        cuestionario.setId(10L);
        cuestionario.setEstado(Cuestionario.EstadoCuestionario.adjudicado);

        combo = new Combo();
        combo.setId(20L);
        combo.setEstado(Combo.EstadoCombo.adjudicado);
        combo.setNivel(Combo.NivelCombo._5LS);
        combo.setTipo(Combo.TipoCombo.P);
        Usuario creador = new Usuario();
        creador.setId(1L);
        combo.setCreacionUsuario(creador);
    }

    @Test
    void registrarAsignacionCuestionarioOk() {
        when(jornadaRepository.findById(1L)).thenReturn(Optional.of(jornada));
        when(cuestionarioRepository.findById(10L)).thenReturn(Optional.of(cuestionario));
        when(historialRepository.save(any(HistorialJornada.class))).thenAnswer(inv -> inv.getArgument(0));

        HistorialJornada resultado = historialJornadaService.registrarAsignacionCuestionario(1L, 10L);

        assertEquals(jornada, resultado.getJornada());
        assertEquals(cuestionario, resultado.getCuestionario());
        assertEquals(TipoAsignacion.CUESTIONARIO, resultado.getTipoAsignacion());
        assertEquals(EstadoAsignacion.asignado, resultado.getEstadoAsignacion());
    }

    @Test
    void registrarAsignacionCuestionarioNotFound() {
        when(jornadaRepository.findById(1L)).thenReturn(Optional.empty());
        when(cuestionarioRepository.findById(10L)).thenReturn(Optional.of(cuestionario));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> historialJornadaService.registrarAsignacionCuestionario(1L, 10L));
        assertEquals("Jornada o cuestionario no encontrado", ex.getMessage());
    }

    @Test
    void registrarAsignacionComboOk() {
        when(jornadaRepository.findById(1L)).thenReturn(Optional.of(jornada));
        when(comboRepository.findById(20L)).thenReturn(Optional.of(combo));
        when(historialRepository.save(any(HistorialJornada.class))).thenAnswer(inv -> inv.getArgument(0));

        HistorialJornada resultado = historialJornadaService.registrarAsignacionCombo(1L, 20L);

        assertEquals(combo, resultado.getCombo());
        assertEquals(TipoAsignacion.COMBO, resultado.getTipoAsignacion());
        assertEquals(EstadoAsignacion.asignado, resultado.getEstadoAsignacion());
    }

    @Test
    void registrarAsignacionComboNotFound() {
        when(jornadaRepository.findById(1L)).thenReturn(Optional.of(jornada));
        when(comboRepository.findById(20L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> historialJornadaService.registrarAsignacionCombo(1L, 20L));
        assertEquals("Jornada o combo no encontrado", ex.getMessage());
    }

    @Test
    void marcarNoUsadosCuestionariosYCombos() {
        HistorialJornada hCuest = new HistorialJornada();
        hCuest.setJornada(jornada);
        hCuest.setCuestionario(cuestionario);
        HistorialJornada hOtra = new HistorialJornada();
        Jornada otra = new Jornada();
        otra.setId(99L);
        hOtra.setJornada(otra);
        hOtra.setCuestionario(cuestionario);

        HistorialJornada hCombo = new HistorialJornada();
        hCombo.setJornada(jornada);
        hCombo.setCombo(combo);

        when(historialRepository.findByCuestionarioId(10L)).thenReturn(Arrays.asList(hCuest, hOtra));
        when(historialRepository.findByComboId(20L)).thenReturn(Collections.singletonList(hCombo));
        when(historialRepository.save(any(HistorialJornada.class))).thenAnswer(inv -> inv.getArgument(0));
        when(cuestionarioRepository.save(any(Cuestionario.class))).thenAnswer(inv -> inv.getArgument(0));
        when(comboRepository.save(any(Combo.class))).thenAnswer(inv -> inv.getArgument(0));

        MarcarNoUsadoDTO dto = new MarcarNoUsadoDTO();
        dto.setJornadaId(1L);
        dto.setCuestionarioIds(Collections.singletonList(10L));
        dto.setComboIds(Collections.singletonList(20L));
        dto.setMotivo("no salió");

        historialJornadaService.marcarNoUsados(dto);

        assertEquals(EstadoAsignacion.no_usado, hCuest.getEstadoAsignacion());
        assertEquals("no salió", hCuest.getNotas());
        assertEquals(Cuestionario.EstadoCuestionario.aprobado, cuestionario.getEstado());
        assertEquals(EstadoAsignacion.no_usado, hCombo.getEstadoAsignacion());
        assertEquals(Combo.EstadoCombo.aprobado, combo.getEstado());
        assertEquals(EstadoAsignacion.asignado, hOtra.getEstadoAsignacion());
        verify(historialRepository, times(2)).save(any(HistorialJornada.class));
    }

    @Test
    void marcarNoUsadosConListasNulasNoHaceNada() {
        MarcarNoUsadoDTO dto = new MarcarNoUsadoDTO();
        dto.setJornadaId(1L);

        historialJornadaService.marcarNoUsados(dto);

        verify(historialRepository, never()).findByCuestionarioId(any());
        verify(historialRepository, never()).findByComboId(any());
    }

    @Test
    void reaprovecharComboOk() {
        HistorialJornada historial = new HistorialJornada();
        historial.setEstadoAsignacion(EstadoAsignacion.asignado);
        historial.setCombo(combo);
        when(comboRepository.findById(20L)).thenReturn(Optional.of(combo));
        when(comboRepository.save(any(Combo.class))).thenAnswer(inv -> {
            Combo c = inv.getArgument(0);
            if (c.getId() == null) {
                c.setId(21L);
            }
            return c;
        });
        when(historialRepository.findByComboId(20L)).thenReturn(Collections.singletonList(historial));
        Pregunta pregunta = new Pregunta();
        pregunta.setId(50L);
        pregunta.setEstadoDisponibilidad(Pregunta.EstadoDisponibilidad.usada);
        when(preguntaRepository.findById(50L)).thenReturn(Optional.of(pregunta));
        when(preguntaRepository.findById(51L)).thenReturn(Optional.empty());
        when(preguntaRepository.save(any(Pregunta.class))).thenAnswer(inv -> inv.getArgument(0));

        ReaprovecharComboDTO dto = new ReaprovecharComboDTO();
        dto.setComboOriginalId(20L);
        dto.setPreguntaUsadaId(40L);
        dto.setPreguntasNoUsadasIds(Arrays.asList(50L, 51L));

        Combo nuevo = historialJornadaService.reaprovecharCombo(dto);

        assertEquals(Combo.EstadoCombo.reaprovechado, combo.getEstado());
        assertEquals(Combo.EstadoCombo.borrador, nuevo.getEstado());
        assertEquals(21L, nuevo.getId());
        assertEquals(EstadoAsignacion.usado, historial.getEstadoAsignacion());
        assertEquals(40L, historial.getPreguntaUsadaId());
        assertEquals(Pregunta.EstadoDisponibilidad.disponible, pregunta.getEstadoDisponibilidad());
        verify(undoService).registrar(any(), any(), any());
    }

    @Test
    void reaprovecharComboNotFound() {
        when(comboRepository.findById(20L)).thenReturn(Optional.empty());
        ReaprovecharComboDTO dto = new ReaprovecharComboDTO();
        dto.setComboOriginalId(20L);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> historialJornadaService.reaprovecharCombo(dto));
        assertEquals("Combo original no encontrado", ex.getMessage());
    }

    @Test
    void reaprovecharComboIgnoraHistorialNoAsignado() {
        HistorialJornada historial = new HistorialJornada();
        historial.setEstadoAsignacion(EstadoAsignacion.usado);
        when(comboRepository.findById(20L)).thenReturn(Optional.of(combo));
        when(comboRepository.save(any(Combo.class))).thenAnswer(inv -> inv.getArgument(0));
        when(historialRepository.findByComboId(20L)).thenReturn(Collections.singletonList(historial));

        ReaprovecharComboDTO dto = new ReaprovecharComboDTO();
        dto.setComboOriginalId(20L);
        dto.setPreguntaUsadaId(1L);

        historialJornadaService.reaprovecharCombo(dto);

        assertEquals(EstadoAsignacion.usado, historial.getEstadoAsignacion());
        verify(historialRepository, never()).save(historial);
    }

    @Test
    void obtenerHistorialCuestionario() {
        HistorialJornada historial = historialCompleto();
        when(historialRepository.findByCuestionarioId(10L)).thenReturn(Collections.singletonList(historial));

        List<HistorialJornadaDTO> dtos = historialJornadaService.obtenerHistorialCuestionario(10L);

        assertEquals(1, dtos.size());
        assertEquals(1L, dtos.get(0).getJornadaId());
        assertEquals("J1", dtos.get(0).getJornadaNombre());
        assertEquals(10L, dtos.get(0).getCuestionarioId());
        assertEquals(20L, dtos.get(0).getComboId());
        assertEquals("CUESTIONARIO", dtos.get(0).getTipoAsignacion());
        assertEquals("asignado", dtos.get(0).getEstadoAsignacion());
    }

    @Test
    void obtenerHistorialCombo() {
        HistorialJornada historial = historialCompleto();
        when(historialRepository.findByComboId(20L)).thenReturn(Collections.singletonList(historial));

        List<HistorialJornadaDTO> dtos = historialJornadaService.obtenerHistorialCombo(20L);

        assertEquals(1, dtos.size());
        assertEquals(20L, dtos.get(0).getComboId());
    }

    @Test
    void obtenerNoUsados() {
        HistorialJornada historial = historialCompleto();
        historial.setEstadoAsignacion(EstadoAsignacion.no_usado);
        when(historialRepository.findByJornadaIdAndEstado(1L, EstadoAsignacion.no_usado))
                .thenReturn(Collections.singletonList(historial));

        List<HistorialJornadaDTO> dtos = historialJornadaService.obtenerNoUsados(1L);

        assertEquals(1, dtos.size());
        assertEquals("no_usado", dtos.get(0).getEstadoAsignacion());
    }

    @Test
    void obtenerHistorialSinCuestionarioNiCombo() {
        HistorialJornada historial = new HistorialJornada();
        historial.setId(3L);
        historial.setJornada(jornada);
        historial.setTipoAsignacion(TipoAsignacion.COMBO);
        historial.setEstadoAsignacion(EstadoAsignacion.asignado);
        when(historialRepository.findByComboId(20L)).thenReturn(Collections.singletonList(historial));

        List<HistorialJornadaDTO> dtos = historialJornadaService.obtenerHistorialCombo(20L);

        assertNull(dtos.get(0).getCuestionarioId());
        assertNull(dtos.get(0).getComboId());
    }

    private HistorialJornada historialCompleto() {
        HistorialJornada historial = new HistorialJornada();
        historial.setId(5L);
        historial.setJornada(jornada);
        historial.setCuestionario(cuestionario);
        historial.setCombo(combo);
        historial.setTipoAsignacion(TipoAsignacion.CUESTIONARIO);
        historial.setEstadoAsignacion(EstadoAsignacion.asignado);
        historial.setNotas("n");
        historial.setPreguntaUsadaId(40L);
        return historial;
    }
}
