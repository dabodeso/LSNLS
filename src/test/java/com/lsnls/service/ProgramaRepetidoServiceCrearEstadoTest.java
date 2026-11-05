package com.lsnls.service;

import com.lsnls.dto.ConcursanteDTO;
import com.lsnls.dto.CrearProgramaRepetidoDTO;
import com.lsnls.dto.ProgramaRepetidoDTO;
import com.lsnls.entity.Concursante;
import com.lsnls.entity.Programa;
import com.lsnls.entity.ProgramaRepetido;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProgramaRepetidoServiceCrearEstadoTest {

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

    private final AtomicReference<ProgramaRepetido> guardado = new AtomicReference<>();

    @BeforeEach
    void setUp() {
        Concursante concursante = new Concursante();
        concursante.setId(10L);
        concursante.setEstado("editado");
        concursante.setNumeroPrograma(1);
        concursante.setNumeroConcursante(42);

        Programa emitido = new Programa();
        emitido.setId(1L);
        emitido.setEstado(Programa.EstadoPrograma.emitido);

        when(concursanteRepository.findById(10L)).thenReturn(Optional.of(concursante));
        when(programaRepository.findById(1L)).thenReturn(Optional.of(emitido));
        when(programaRepetidoRepository.saveAndFlush(any(ProgramaRepetido.class))).thenAnswer(inv -> {
            ProgramaRepetido repetido = inv.getArgument(0);
            repetido.setId(20L);
            guardado.set(repetido);
            return repetido;
        });
        when(programaRepetidoRepository.findById(20L)).thenAnswer(inv -> Optional.of(guardado.get()));
        when(programaRepetidoRepository.save(any(ProgramaRepetido.class))).thenAnswer(inv -> inv.getArgument(0));
        when(enlaceRepository.existsByProgramaRepetidoIdAndConcursanteId(20L, 10L)).thenReturn(false);
        when(enlaceRepository.existsByProgramaRepetidoIdAndOrdenEscaleta(20L, 1)).thenReturn(false);
        when(enlaceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(enlaceRepository.countByProgramaRepetidoId(20L)).thenReturn(1L);
        when(concursanteService.copiarDto(any(Concursante.class))).thenReturn(new ConcursanteDTO());
    }

    @Test
    void crear_sinEstadoQuedaEditadoAunqueTengaFecha() {
        LocalDate fecha = LocalDate.of(2026, 11, 5);
        when(programaRepetidoRepository.existsByTipoAndCodigoAndTemporadaAndFechaEmision("REPE", "4", 2, fecha))
                .thenReturn(false);

        ProgramaRepetidoDTO dto = service.crear(alta(fecha, null));

        assertEquals("editado", dto.getEstado());
        assertEquals(fecha, dto.getFechaEmision());
    }

    @Test
    void crear_sinEstadoNiFechaQuedaEditado() {
        when(programaRepetidoRepository.existsByTipoAndCodigoAndTemporadaAndFechaEmisionIsNull("REPE", "4", 2))
                .thenReturn(false);

        ProgramaRepetidoDTO dto = service.crear(alta(null, "  "));

        assertEquals("editado", dto.getEstado());
        assertNull(dto.getFechaEmision());
    }

    @Test
    void crear_estadoExplicitoSeRespeta() {
        when(programaRepetidoRepository.existsByTipoAndCodigoAndTemporadaAndFechaEmisionIsNull("REPE", "4", 2))
                .thenReturn(false);

        ProgramaRepetidoDTO dto = service.crear(alta(null, "borrador"));

        assertEquals("borrador", dto.getEstado());
    }

    private static CrearProgramaRepetidoDTO alta(LocalDate fecha, String estado) {
        CrearProgramaRepetidoDTO dto = new CrearProgramaRepetidoDTO();
        dto.setTipo("REPE");
        dto.setTemporada(2);
        dto.setCodigo("4");
        dto.setFechaEmision(fecha);
        dto.setEstado(estado);
        dto.setConcursanteIds(List.of(10L));
        return dto;
    }
}
