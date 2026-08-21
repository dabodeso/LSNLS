package com.lsnls.service;

import com.lsnls.dto.ProgramaDTO;
import com.lsnls.entity.Programa;
import com.lsnls.repository.ProgramaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProgramaServiceTest {

    @Mock
    private ProgramaRepository programaRepository;
    @Mock
    private ConfiguracionGlobalService configuracionService;
    @Mock
    private EntityManager entityManager;
    @Mock
    private UndoService undoService;

    @InjectMocks
    private ProgramaService service;

    private Programa programa;

    @BeforeEach
    void setUp() {
        programa = new Programa();
        programa.setId(1L);
        programa.setCodigo("P01");
        programa.setTemporada(1);
        programa.setEstado(Programa.EstadoPrograma.borrador);
        programa.setDuracionObjetivo("45m");
    }

    @SuppressWarnings("unchecked")
    private void mockConcursantesCount(long count) {
        TypedQuery<Long> query = mock(TypedQuery.class);
        when(entityManager.createQuery(anyString(), eq(Long.class))).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.getSingleResult()).thenReturn(count);
    }

    private void stubSaveConId() {
        when(programaRepository.save(any(Programa.class))).thenAnswer(inv -> {
            Programa p = inv.getArgument(0);
            if (p.getId() == null) {
                p.setId(1L);
            }
            return p;
        });
    }

    @Test
    void findAllYFindById() {
        when(programaRepository.findAll()).thenReturn(Collections.singletonList(programa));
        when(programaRepository.findById(1L)).thenReturn(Optional.of(programa));
        when(programaRepository.findById(9L)).thenReturn(Optional.empty());

        assertEquals(1, service.findAll().size());
        assertTrue(service.findById(1L).isPresent());
        assertTrue(service.findById(9L).isEmpty());
        assertEquals(1, service.findAllDTO().size());
        assertTrue(service.findByIdDTO(1L).isPresent());
        assertEquals("P01", service.findByIdDTO(1L).get().getCodigo());
    }

    @Test
    void findAllPaginated() {
        Page<Programa> page = new PageImpl<>(Collections.singletonList(programa), PageRequest.of(0, 10), 1);
        when(programaRepository.findAll(any(org.springframework.data.domain.Pageable.class))).thenReturn(page);

        Map<String, Object> response = service.findAllPaginated(PageRequest.of(0, 10));
        assertEquals(1, ((List<?>) response.get("programas")).size());
        assertEquals(0, response.get("currentPage"));
        assertEquals(1L, response.get("totalItems"));
        assertEquals(1, response.get("totalPages"));
    }

    @Test
    void create_asignaDuracionYCodigoPorDefecto() {
        when(configuracionService.getDuracionObjetivo()).thenReturn("45m");
        stubSaveConId();
        Programa nuevo = new Programa();
        nuevo.setTemporada(2);
        nuevo.setDuracionObjetivo(null);

        Programa saved = service.create(nuevo);
        assertEquals("45m", saved.getDuracionObjetivo());
        assertEquals("1", saved.getCodigo());
        assertEquals(Programa.EstadoPrograma.borrador, saved.getEstado());
    }

    @Test
    void create_codigoDuplicado() {
        Programa nuevo = new Programa();
        nuevo.setCodigo("P01");
        when(programaRepository.findByCodigo("P01")).thenReturn(Optional.of(programa));
        assertThrows(IllegalArgumentException.class, () -> service.create(nuevo));
    }

    @Test
    void create_conservaDuracionSiYaTiene() {
        stubSaveConId();
        Programa nuevo = new Programa();
        nuevo.setCodigo("PX");
        nuevo.setDuracionObjetivo("1h");
        when(programaRepository.findByCodigo("PX")).thenReturn(Optional.empty());

        Programa saved = service.create(nuevo);
        assertEquals("1h", saved.getDuracionObjetivo());
        assertEquals("PX", saved.getCodigo());
    }

    @Test
    void createFromDTO() {
        when(configuracionService.getDuracionObjetivo()).thenReturn("45m");
        stubSaveConId();
        ProgramaDTO dto = new ProgramaDTO();
        dto.setCodigo("D1");
        dto.setTemporada(3);
        dto.setEstado("borrador");
        dto.setTotalPremios(new BigDecimal("10"));
        when(programaRepository.findByCodigo("D1")).thenReturn(Optional.empty());

        ProgramaDTO saved = service.createFromDTO(dto);
        assertNotNull(saved.getId());
        assertEquals("D1", saved.getCodigo());
        assertEquals("borrador", saved.getEstado());
    }

    @Test
    void createFromDTO_estadoInvalido_usaBorrador() {
        when(configuracionService.getDuracionObjetivo()).thenReturn("45m");
        stubSaveConId();
        ProgramaDTO dto = new ProgramaDTO();
        dto.setCodigo("D2");
        dto.setEstado("no_existe");
        when(programaRepository.findByCodigo("D2")).thenReturn(Optional.empty());

        ProgramaDTO saved = service.createFromDTO(dto);
        assertEquals("borrador", saved.getEstado());
    }

    @Test
    void updateYUpdateFromDTO() {
        stubSaveConId();
        Programa updated = service.update(1L, programa);
        assertEquals(1L, updated.getId());

        ProgramaDTO dto = new ProgramaDTO();
        dto.setCodigo("P99");
        dto.setTemporada(1);
        dto.setEstado("grabado");
        when(programaRepository.existsByCodigoAndIdNot("P99", 1L)).thenReturn(false);
        ProgramaDTO dtoSaved = service.updateFromDTO(1L, dto);
        assertEquals("P99", dtoSaved.getCodigo());
        assertEquals("borrador", dtoSaved.getEstado());
    }

    @Test
    void updateFromDTO_codigoObligatorioYDuplicadoYLargo() {
        ProgramaDTO sinCodigo = new ProgramaDTO();
        assertThrows(IllegalArgumentException.class, () -> service.updateFromDTO(1L, sinCodigo));

        ProgramaDTO largo = new ProgramaDTO();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 33; i++) {
            sb.append("A");
        }
        largo.setCodigo(sb.toString());
        assertThrows(IllegalArgumentException.class, () -> service.updateFromDTO(1L, largo));

        ProgramaDTO dup = new ProgramaDTO();
        dup.setCodigo("P01");
        when(programaRepository.existsByCodigoAndIdNot("P01", 1L)).thenReturn(true);
        assertThrows(IllegalArgumentException.class, () -> service.updateFromDTO(1L, dup));
    }

    @Test
    void updateCampo_variosKeys() {
        when(programaRepository.findById(1L)).thenReturn(Optional.of(programa));
        stubSaveConId();
        when(programaRepository.existsByCodigoAndIdNot("NEW", 1L)).thenReturn(false);

        Map<String, Object> campos = new HashMap<>();
        campos.put("codigo", "NEW");
        campos.put("temporada", "4");
        campos.put("totalPremios", "12.50");
        campos.put("creditosEspeciales", "cred");
        campos.put("notas", "nota");
        campos.put("resultadoAcumulado", "3.2");
        campos.put("duracionAcumulada", "01:02:03");
        campos.put("fechaEmision", "2024-05-01");
        campos.put("desconocido", "x");

        ProgramaDTO dto = service.updateCampo(1L, campos);
        assertEquals("NEW", dto.getCodigo());
        assertEquals(4, dto.getTemporada());
        assertEquals(new BigDecimal("12.50"), dto.getTotalPremios());
        assertEquals("cred", dto.getCreditosEspeciales());
        assertEquals("nota", dto.getNotas());
        assertEquals(new BigDecimal("3.2"), dto.getResultadoAcumulado());
        assertEquals(LocalTime.parse("01:02:03"), dto.getDuracionAcumulada());
        assertEquals(LocalDate.parse("2024-05-01"), dto.getFechaEmision());
        assertEquals("programado", dto.getEstado());
    }

    @Test
    void updateCampo_estadoExplicitoYParseosInvalidosYNulls() {
        when(programaRepository.findById(1L)).thenReturn(Optional.of(programa));
        stubSaveConId();

        Map<String, Object> campos = new HashMap<>();
        campos.put("temporada", null);
        campos.put("totalPremios", null);
        campos.put("resultadoAcumulado", null);
        campos.put("duracionAcumulada", "no-es-hora");
        campos.put("fechaEmision", "fecha-mala");
        campos.put("estado", "emitido");

        ProgramaDTO dto = service.updateCampo(1L, campos);
        assertNull(dto.getDuracionAcumulada());
        assertNull(dto.getFechaEmision());
        assertEquals("emitido", dto.getEstado());
    }

    @Test
    void updateCampo_estadoInvalidoYVacios() {
        when(programaRepository.findById(1L)).thenReturn(Optional.of(programa));
        stubSaveConId();

        Map<String, Object> campos = new HashMap<>();
        campos.put("duracionAcumulada", "");
        campos.put("fechaEmision", "");
        campos.put("estado", "");
        campos.put("estadoInvalido", "x");
        campos.put("estado", "NOPE");

        ProgramaDTO dto = service.updateCampo(1L, campos);
        assertNull(dto.getDuracionAcumulada());
        assertNull(dto.getFechaEmision());
    }

    @Test
    void updateCampo_noEncontrado() {
        when(programaRepository.findById(5L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> service.updateCampo(5L, new HashMap<>()));
    }

    @Test
    void delete_noEncontrado() {
        when(programaRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> service.delete(1L));
    }

    @Test
    void delete_conConcursantes() {
        when(programaRepository.findById(1L)).thenReturn(Optional.of(programa));
        mockConcursantesCount(3L);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.delete(1L));
        assertTrue(ex.getMessage().contains("concursante"));
        verify(programaRepository, never()).deleteById(1L);
    }

    @Test
    void delete_programado() {
        programa.setEstado(Programa.EstadoPrograma.programado);
        when(programaRepository.findById(1L)).thenReturn(Optional.of(programa));
        mockConcursantesCount(0L);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.delete(1L));
        assertTrue(ex.getMessage().contains("programado"));
    }

    @Test
    void delete_emitido() {
        programa.setEstado(Programa.EstadoPrograma.emitido);
        when(programaRepository.findById(1L)).thenReturn(Optional.of(programa));
        mockConcursantesCount(0L);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.delete(1L));
        assertTrue(ex.getMessage().contains("emitido"));
    }

    @Test
    void delete_okConUndo() {
        when(programaRepository.findById(1L)).thenReturn(Optional.of(programa));
        mockConcursantesCount(0L);
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("id", 1L);
        when(undoService.snapshotFila("programas", 1L)).thenReturn(snapshot);

        service.delete(1L);

        verify(programaRepository).deleteById(1L);
        verify(undoService).registrar(eq("eliminar_programa"), anyString(), anyList());
    }

    @Test
    void delete_okSinSnapshot() {
        when(programaRepository.findById(1L)).thenReturn(Optional.of(programa));
        mockConcursantesCount(0L);
        when(undoService.snapshotFila("programas", 1L)).thenReturn(null);

        service.delete(1L);

        verify(programaRepository).deleteById(1L);
        verify(undoService, never()).registrar(anyString(), anyString(), anyList());
    }

    @Test
    void getDuracionObjetivoYUpdate() {
        when(configuracionService.getDuracionObjetivo()).thenReturn("45m");
        assertEquals("45m", service.getDuracionObjetivo());

        when(programaRepository.findById(1L)).thenReturn(Optional.of(programa));
        service.updateDuracionObjetivo(1L, "1h");
        verify(programaRepository).save(programa);
        assertEquals("1h", programa.getDuracionObjetivo());

        when(programaRepository.findById(8L)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> service.updateDuracionObjetivo(8L, "1h"));
    }
}
