package com.lsnls.service;

import com.lsnls.dto.JornadaDTO;
import com.lsnls.dto.ReciclajeComboDTO;
import com.lsnls.entity.Combo;
import com.lsnls.entity.Cuestionario;
import com.lsnls.entity.Jornada;
import com.lsnls.entity.Pregunta;
import com.lsnls.entity.PreguntaCombo;
import com.lsnls.entity.Usuario;
import com.lsnls.repository.ComboRepository;
import com.lsnls.repository.CuestionarioRepository;
import com.lsnls.repository.JornadaRepository;
import com.lsnls.repository.PreguntaComboRepository;
import com.lsnls.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class JornadaServiceTest {

    @Mock private JornadaRepository jornadaRepository;
    @Mock private CuestionarioRepository cuestionarioRepository;
    @Mock private ComboRepository comboRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private ExcelExportService excelExportService;
    @Mock private CuestionarioService cuestionarioService;
    @Mock private ComboService comboService;
    @Mock private PreguntaComboRepository preguntaComboRepository;
    @Mock private EntityManager entityManager;
    @Mock private UndoService undoService;
    @Mock private TypedQuery<Object> typedQuery;
    @Mock private Query nativeQuery;

    @InjectMocks
    private JornadaService jornadaService;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        when(entityManager.createQuery(anyString(), any(Class.class))).thenReturn((TypedQuery) typedQuery);
        when(entityManager.createQuery(anyString())).thenReturn(typedQuery);
        when(typedQuery.setParameter(anyString(), nullable(Object.class))).thenReturn(typedQuery);
        when(typedQuery.setParameter(anyInt(), nullable(Object.class))).thenReturn(typedQuery);
        when(typedQuery.setMaxResults(anyInt())).thenReturn(typedQuery);
        when(typedQuery.getSingleResult()).thenReturn(0L);
        when(typedQuery.getResultList()).thenReturn(Collections.emptyList());

        when(entityManager.createNativeQuery(anyString())).thenReturn(nativeQuery);
        when(nativeQuery.setParameter(anyString(), nullable(Object.class))).thenReturn(nativeQuery);
        when(nativeQuery.setParameter(anyInt(), nullable(Object.class))).thenReturn(nativeQuery);
        when(nativeQuery.getSingleResult()).thenReturn(0L);
        when(nativeQuery.getResultList()).thenReturn(Collections.emptyList());
        when(nativeQuery.executeUpdate()).thenReturn(1);

        when(undoService.snapshotFila(anyString(), any())).thenReturn(Collections.singletonMap("id", 1L));
        when(undoService.snapshotFilas(anyString(), anyString(), any())).thenReturn(Collections.emptyList());
        when(jornadaRepository.save(any(Jornada.class))).thenAnswer(inv -> {
            Jornada j = inv.getArgument(0);
            if (j.getId() == null) {
                j.setId(1L);
            }
            return j;
        });
    }

    private Usuario usuarioMinimo() {
        Usuario u = new Usuario();
        u.setId(10L);
        u.setNombre("admin");
        u.setRol(Usuario.RolUsuario.ROLE_ADMIN);
        return u;
    }

    private Jornada jornadaBase() {
        Jornada j = new Jornada();
        j.setId(1L);
        j.setNombre("Jornada 1");
        j.setLugar("Madrid");
        j.setFechaJornada(LocalDate.of(2026, 1, 15));
        j.setEstado(Jornada.EstadoJornada.preparacion);
        j.setCreacionUsuario(usuarioMinimo());
        j.setNotas("notas");
        return j;
    }

    private JornadaDTO dtoMinimo() {
        JornadaDTO dto = new JornadaDTO();
        dto.setNombre("Jornada nueva");
        dto.setLugar("Sevilla");
        dto.setFechaJornada(LocalDate.of(2026, 3, 1));
        dto.setNotas("ok");
        return dto;
    }

    @Test
    void obtenerTodas_devuelveDtos() {
        when(jornadaRepository.findAllOrderByFechaCreacionDesc()).thenReturn(Collections.singletonList(jornadaBase()));

        List<JornadaDTO> result = jornadaService.obtenerTodas();

        assertEquals(1, result.size());
        assertEquals("Jornada 1", result.get(0).getNombre());
        assertEquals("preparacion", result.get(0).getEstado());
        assertEquals(10L, result.get(0).getCreacionUsuarioId());
    }

    @Test
    void obtenerPorId_empty() {
        when(jornadaRepository.findById(99L)).thenReturn(Optional.empty());
        assertFalse(jornadaService.obtenerPorId(99L).isPresent());
    }

    @Test
    void obtenerPorId_present() {
        when(jornadaRepository.findById(1L)).thenReturn(Optional.of(jornadaBase()));
        Optional<JornadaDTO> result = jornadaService.obtenerPorId(1L);
        assertTrue(result.isPresent());
        assertEquals(1L, result.get().getId());
        assertEquals("admin", result.get().getCreacionUsuarioNombre());
    }

    @Test
    void crear_usuarioNoExiste() {
        when(jornadaRepository.existsByNombre("Jornada nueva")).thenReturn(false);
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> jornadaService.crear(dtoMinimo(), 99L));
        assertTrue(ex.getMessage().contains("Usuario no encontrado"));
    }

    @Test
    void crear_nombreDuplicado() {
        when(jornadaRepository.existsByNombre("Jornada nueva")).thenReturn(true);
        assertThrows(IllegalArgumentException.class, () -> jornadaService.crear(dtoMinimo(), 10L));
    }

    @Test
    void crear_okMinimo() {
        when(jornadaRepository.existsByNombre("Jornada nueva")).thenReturn(false);
        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(usuarioMinimo()));

        JornadaDTO result = jornadaService.crear(dtoMinimo(), 10L);

        assertNotNull(result.getId());
        assertEquals("Jornada nueva", result.getNombre());
        assertEquals("preparacion", result.getEstado());
        assertEquals(10L, result.getCreacionUsuarioId());
        verify(jornadaRepository).save(any(Jornada.class));
    }

    @Test
    void crear_demasiadosCuestionarios() {
        JornadaDTO dto = dtoMinimo();
        dto.setCuestionarioIds(Arrays.asList(1L, 2L, 3L, 4L, 5L, 6L, 7L));
        when(jornadaRepository.existsByNombre(anyString())).thenReturn(false);
        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(usuarioMinimo()));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> jornadaService.crear(dto, 10L));
        assertTrue(ex.getMessage().contains("Máximo 6"));
    }

    @Test
    void actualizar_noEncontrada() {
        when(jornadaRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> jornadaService.actualizar(1L, dtoMinimo()));
    }

    @Test
    void actualizar_completadaNoEditable() {
        Jornada j = jornadaBase();
        j.setEstado(Jornada.EstadoJornada.completada);
        when(jornadaRepository.findById(1L)).thenReturn(Optional.of(j));
        assertThrows(IllegalArgumentException.class, () -> jornadaService.actualizar(1L, dtoMinimo()));
    }

    @Test
    void actualizar_ok() {
        when(jornadaRepository.findById(1L)).thenReturn(Optional.of(jornadaBase()));
        JornadaDTO dto = dtoMinimo();
        dto.setNombre("Jornada editada");

        JornadaDTO result = jornadaService.actualizar(1L, dto);

        assertEquals("Jornada editada", result.getNombre());
        assertEquals("Sevilla", result.getLugar());
        verify(jornadaRepository).save(any(Jornada.class));
    }

    @Test
    void eliminar_noExiste() {
        when(jornadaRepository.findById(5L)).thenReturn(Optional.empty());
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> jornadaService.eliminar(5L));
        assertTrue(ex.getMessage().contains("no encontrada"));
    }

    @Test
    void eliminar_archivadaNoPermitido() {
        Jornada j = jornadaBase();
        j.setEstado(Jornada.EstadoJornada.archivada);
        when(jornadaRepository.findById(1L)).thenReturn(Optional.of(j));
        assertThrows(IllegalArgumentException.class, () -> jornadaService.eliminar(1L));
    }

    @Test
    void eliminar_conConcursantesAsignados() {
        when(jornadaRepository.findById(1L)).thenReturn(Optional.of(jornadaBase()));
        when(typedQuery.getSingleResult()).thenReturn(3L);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> jornadaService.eliminar(1L));
        assertTrue(ex.getMessage().contains("concursante"));
    }

    @Test
    void eliminar_ok() {
        Jornada j = jornadaBase();
        Cuestionario c = new Cuestionario();
        c.setId(20L);
        c.setEstado(Cuestionario.EstadoCuestionario.adjudicado);
        c.setNivel(Cuestionario.NivelCuestionario._1LS);
        Combo combo = new Combo();
        combo.setId(30L);
        combo.setEstado(Combo.EstadoCombo.adjudicado);
        combo.setNivel(Combo.NivelCombo._5LS);
        j.setCuestionarios(new HashSet<>(Collections.singletonList(c)));
        j.setCombos(new HashSet<>(Collections.singletonList(combo)));
        when(jornadaRepository.findById(1L)).thenReturn(Optional.of(j));

        jornadaService.eliminar(1L);

        verify(jornadaRepository).delete(j);
        verify(undoService).snapshotFila(eq("jornadas"), eq(1L));
        verify(undoService).registrar(eq("eliminar_jornada"), anyString(), any());
        assertEquals(Cuestionario.EstadoCuestionario.aprobado, c.getEstado());
        assertEquals(Combo.EstadoCombo.aprobado, combo.getEstado());
    }

    @Test
    void cambiarEstado_noEncontrada() {
        when(jornadaRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> jornadaService.cambiarEstado(1L, "lista"));
    }

    @Test
    void cambiarEstado_invalido() {
        when(jornadaRepository.findById(1L)).thenReturn(Optional.of(jornadaBase()));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> jornadaService.cambiarEstado(1L, "no_existe"));
        assertTrue(ex.getMessage().contains("Estado no válido"));
    }

    @Test
    void cambiarEstado_aListaMarcaAdjudicado() {
        Jornada j = jornadaBase();
        Cuestionario c = new Cuestionario();
        c.setId(20L);
        c.setEstado(Cuestionario.EstadoCuestionario.aprobado);
        c.setNivel(Cuestionario.NivelCuestionario.NORMAL);
        Combo combo = new Combo();
        combo.setId(30L);
        combo.setEstado(Combo.EstadoCombo.aprobado);
        combo.setNivel(Combo.NivelCombo.NORMAL);
        j.setCuestionarios(new HashSet<>(Collections.singletonList(c)));
        j.setCombos(new HashSet<>(Collections.singletonList(combo)));
        when(jornadaRepository.findById(1L)).thenReturn(Optional.of(j));

        JornadaDTO result = jornadaService.cambiarEstado(1L, "lista");

        assertEquals("lista", result.getEstado());
        assertEquals(Cuestionario.EstadoCuestionario.adjudicado, c.getEstado());
        assertEquals(Combo.EstadoCombo.adjudicado, combo.getEstado());
        verify(undoService).registrar(eq("cambiar_estado_jornada"), anyString(), any());
    }

    @Test
    void cambiarEstado_aArchivadaMarcaGrabado() {
        Jornada j = jornadaBase();
        Cuestionario c = new Cuestionario();
        c.setId(20L);
        c.setEstado(Cuestionario.EstadoCuestionario.adjudicado);
        c.setNivel(Cuestionario.NivelCuestionario.NORMAL);
        Combo combo = new Combo();
        combo.setId(30L);
        combo.setEstado(Combo.EstadoCombo.adjudicado);
        combo.setNivel(Combo.NivelCombo.NORMAL);
        j.setCuestionarios(new HashSet<>(Collections.singletonList(c)));
        j.setCombos(new HashSet<>(Collections.singletonList(combo)));
        when(jornadaRepository.findById(1L)).thenReturn(Optional.of(j));

        JornadaDTO result = jornadaService.cambiarEstado(1L, "archivada");

        assertEquals("archivada", result.getEstado());
        assertEquals(Cuestionario.EstadoCuestionario.grabado, c.getEstado());
        assertEquals(Combo.EstadoCombo.grabado, combo.getEstado());
    }

    @Test
    void exportarExcel_ok() throws Exception {
        Jornada j = jornadaBase();
        when(jornadaRepository.findById(1L)).thenReturn(Optional.of(j));
        when(excelExportService.exportarJornada(eq(j), nullable(Map.class))).thenReturn(new byte[]{1, 2, 3});

        byte[] bytes = jornadaService.exportarExcel(1L);

        assertEquals(3, bytes.length);
        verify(excelExportService).exportarJornada(eq(j), nullable(Map.class));
    }

    @Test
    void exportarExcel_noEncontradaEnvuelveRuntime() {
        when(jornadaRepository.findById(1L)).thenReturn(Optional.empty());
        RuntimeException ex = assertThrows(RuntimeException.class, () -> jornadaService.exportarExcel(1L));
        assertTrue(ex.getMessage().contains("Error al generar Excel"));
    }

    @Test
    void obtenerCuestionariosDisponibles() {
        Cuestionario c = new Cuestionario();
        c.setId(8L);
        c.setNivel(Cuestionario.NivelCuestionario._1LS);
        c.setEstado(Cuestionario.EstadoCuestionario.aprobado);
        c.setTematica("Historia");
        c.setPreguntas(null);
        when(cuestionarioRepository.findByEstado(Cuestionario.EstadoCuestionario.aprobado))
            .thenReturn(Collections.singletonList(c));

        List<Map<String, Object>> result = jornadaService.obtenerCuestionariosDisponibles();

        assertEquals(1, result.size());
        assertEquals(8L, result.get(0).get("id"));
        assertEquals("_1LS", result.get(0).get("nivel"));
        assertEquals(0, result.get(0).get("totalPreguntas"));
    }

    @Test
    void obtenerCombosDisponibles() {
        Combo c = new Combo();
        c.setId(9L);
        c.setNivel(Combo.NivelCombo._5NLS);
        c.setEstado(Combo.EstadoCombo.aprobado);
        c.setTipo(Combo.TipoCombo.P);
        c.setTematica("Cine");
        when(comboRepository.findByEstado(Combo.EstadoCombo.aprobado)).thenReturn(Collections.singletonList(c));

        List<Map<String, Object>> result = jornadaService.obtenerCombosDisponibles();

        assertEquals(1, result.size());
        assertEquals(9L, result.get(0).get("id"));
        assertEquals("P", result.get(0).get("tipo"));
        assertEquals(0, result.get(0).get("totalPreguntas"));
    }

    @Test
    void esComboDerivado_falseYtrue() {
        when(nativeQuery.getSingleResult()).thenReturn(0L);
        assertFalse(jornadaService.esComboDerivado(4L));

        when(nativeQuery.getSingleResult()).thenReturn(2L);
        assertTrue(jornadaService.esComboDerivado(4L));
    }

    @Test
    void esComboDerivadoDeJornada_falseYtrue() {
        when(nativeQuery.getSingleResult()).thenReturn(0L);
        assertFalse(jornadaService.esComboDerivadoDeJornada(1L, 4L));

        when(nativeQuery.getSingleResult()).thenReturn(1L);
        assertTrue(jornadaService.esComboDerivadoDeJornada(1L, 4L));
    }

    @Test
    void reutilizarCuestionario_adjudicado() {
        Jornada jornada = new Jornada();
        jornada.setId(1L);
        Cuestionario cuest = new Cuestionario();
        cuest.setId(2L);
        cuest.setEstado(Cuestionario.EstadoCuestionario.adjudicado);
        jornada.setCuestionarios(new HashSet<>(Collections.singletonList(cuest)));
        when(jornadaRepository.findById(1L)).thenReturn(Optional.of(jornada));
        when(cuestionarioRepository.findById(2L)).thenReturn(Optional.of(cuest));
        when(cuestionarioService.cambiarEstadoAtomico(eq(2L),
            eq(Cuestionario.EstadoCuestionario.adjudicado),
            eq(Cuestionario.EstadoCuestionario.aprobado))).thenReturn(true);
        when(nativeQuery.getSingleResult()).thenReturn(0L);

        jornadaService.reutilizarCuestionario(1L, 2L, 4L);
        verify(cuestionarioService).cambiarEstadoAtomico(eq(2L),
            eq(Cuestionario.EstadoCuestionario.adjudicado),
            eq(Cuestionario.EstadoCuestionario.aprobado));
    }

    @Test
    @SuppressWarnings("unchecked")
    void reciclarComboEntero_registraUndo() {
        Jornada jornada = new Jornada();
        jornada.setId(1L);
        Combo combo = new Combo();
        combo.setId(3L);
        combo.setEstado(Combo.EstadoCombo.adjudicado);
        combo.setPreguntas(new HashSet<>(Arrays.asList(
                new com.lsnls.entity.PreguntaCombo(),
                new com.lsnls.entity.PreguntaCombo(),
                new com.lsnls.entity.PreguntaCombo())));
        jornada.setCombos(new HashSet<>(Collections.singletonList(combo)));
        when(jornadaRepository.findById(1L)).thenReturn(Optional.of(jornada));
        when(comboRepository.findById(3L)).thenReturn(Optional.of(combo));
        when(comboRepository.save(any(Combo.class))).thenAnswer(inv -> inv.getArgument(0));
        when(undoService.snapshotFilas(anyString(), anyString(), any())).thenReturn(Collections.emptyList());
        when(undoService.snapshotFilasNuevas(anyString(), anyString(), any(), any())).thenReturn(Collections.emptyList());

        jornadaService.reciclarComboEntero(1L, 3L, 9L);

        assertEquals(Combo.EstadoCombo.aprobado, combo.getEstado());
        ArgumentCaptor<List<Map<String, Object>>> captor = ArgumentCaptor.forClass(List.class);
        verify(undoService).registrar(eq("reciclar_combo_entero"), anyString(), captor.capture());
        List<Map<String, Object>> acciones = captor.getValue();
        assertTrue(acciones.stream().anyMatch(a ->
                "actualizar_campos".equals(a.get("tipo"))
                        && "combos".equals(a.get("tabla"))
                        && Long.valueOf(3L).equals(((Number) a.get("id")).longValue())));
        assertTrue(deshacerAccionesConUndoReal(acciones).stream()
                .anyMatch(sql -> sql.contains("UPDATE combos SET") && sql.contains("`estado`")));
    }

    @Test
    void reciclarComboEntero_noAsignadoAJornada() {
        Jornada jornada = new Jornada();
        jornada.setId(1L);
        Combo asignado = new Combo();
        asignado.setId(8L);
        jornada.setCombos(new HashSet<>(Collections.singletonList(asignado)));
        Combo otro = new Combo();
        otro.setId(3L);
        otro.setEstado(Combo.EstadoCombo.adjudicado);
        when(jornadaRepository.findById(1L)).thenReturn(Optional.of(jornada));
        when(comboRepository.findById(3L)).thenReturn(Optional.of(otro));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> jornadaService.reciclarComboEntero(1L, 3L, 9L));
        assertTrue(ex.getMessage().contains("no está asignado"));
    }

    @Test
    void reciclarComboEntero_estadoIncorrecto() {
        Jornada jornada = new Jornada();
        jornada.setId(1L);
        Combo combo = new Combo();
        combo.setId(3L);
        combo.setEstado(Combo.EstadoCombo.aprobado);
        combo.setPreguntas(new HashSet<>(Arrays.asList(
            new PreguntaCombo(), new PreguntaCombo(), new PreguntaCombo())));
        jornada.setCombos(new HashSet<>(Collections.singletonList(combo)));
        when(jornadaRepository.findById(1L)).thenReturn(Optional.of(jornada));
        when(comboRepository.findById(3L)).thenReturn(Optional.of(combo));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> jornadaService.reciclarComboEntero(1L, 3L, 9L));
        assertTrue(ex.getMessage().contains("adjudicado"));
    }

    @Test
    void reciclarComboEntero_sinTresPreguntas() {
        Jornada jornada = new Jornada();
        jornada.setId(1L);
        Combo combo = new Combo();
        combo.setId(3L);
        combo.setEstado(Combo.EstadoCombo.grabado);
        combo.setPreguntas(new HashSet<>());
        jornada.setCombos(new HashSet<>(Collections.singletonList(combo)));
        when(jornadaRepository.findById(1L)).thenReturn(Optional.of(jornada));
        when(comboRepository.findById(3L)).thenReturn(Optional.of(combo));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> jornadaService.reciclarComboEntero(1L, 3L, 9L));
        assertTrue(ex.getMessage().contains("exactamente 3"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void reciclarComboParcial_registraUndo() {
        Jornada jornada = new Jornada();
        jornada.setId(1L);
        Combo combo = new Combo();
        combo.setId(3L);
        combo.setEstado(Combo.EstadoCombo.adjudicado);
        combo.setNivel(Combo.NivelCombo.NORMAL);
        combo.setTipo(Combo.TipoCombo.P);
        Usuario creador = new Usuario();
        creador.setId(1L);
        combo.setCreacionUsuario(creador);

        Pregunta p1 = new Pregunta();
        p1.setId(10L);
        Pregunta p2 = new Pregunta();
        p2.setId(11L);
        Pregunta p3 = new Pregunta();
        p3.setId(12L);
        combo.setPreguntas(new HashSet<>(Arrays.asList(
                preguntaCombo(combo, p1, 1, "2"),
                preguntaCombo(combo, p2, 2, "3"),
                preguntaCombo(combo, p3, 3, "X"))));
        jornada.setCombos(new HashSet<>(Collections.singletonList(combo)));
        when(jornadaRepository.findById(1L)).thenReturn(Optional.of(jornada));
        when(comboRepository.findById(3L)).thenReturn(Optional.of(combo));
        when(comboRepository.save(any(Combo.class))).thenAnswer(inv -> {
            Combo c = inv.getArgument(0);
            if (c.getId() == null) {
                c.setId(99L);
            }
            return c;
        });
        when(preguntaComboRepository.save(any(PreguntaCombo.class))).thenAnswer(inv -> inv.getArgument(0));
        when(undoService.snapshotFilas(anyString(), anyString(), any())).thenReturn(Collections.emptyList());
        when(undoService.snapshotFilasNuevas(anyString(), anyString(), any(), any())).thenReturn(Collections.emptyList());
        when(nativeQuery.getSingleResult()).thenReturn(0L);

        ReciclajeComboDTO dto = jornadaService.reciclarComboParcial(1L, 3L, 10L, 5L);

        assertEquals(99L, dto.getComboHijoId());
        assertEquals(3L, dto.getComboPadreId());
        assertEquals(Combo.EstadoCombo.adjudicado, combo.getEstado());
        ArgumentCaptor<List<Map<String, Object>>> captor = ArgumentCaptor.forClass(List.class);
        verify(undoService).registrar(eq("reciclar_combo_parcial"), anyString(), captor.capture());
        List<Map<String, Object>> acciones = captor.getValue();
        assertTrue(acciones.stream().anyMatch(a ->
                "eliminar_fila".equals(a.get("tipo")) && "combos".equals(a.get("tabla"))));
        assertTrue(acciones.stream().anyMatch(a ->
                "eliminar_filas".equals(a.get("tipo")) && "combos_preguntas".equals(a.get("tabla"))));
        List<String> sqls = deshacerAccionesConUndoReal(acciones);
        assertTrue(sqls.stream().anyMatch(sql -> sql.contains("DELETE FROM combos_preguntas")));
        assertTrue(sqls.stream().anyMatch(sql -> sql.contains("DELETE FROM combos WHERE id = ?")));
    }

    @Test
    void reciclarComboParcial_preguntaQueNoEstaEnElCombo() {
        Jornada jornada = new Jornada();
        jornada.setId(1L);
        Combo combo = new Combo();
        combo.setId(3L);
        combo.setEstado(Combo.EstadoCombo.adjudicado);
        Pregunta p1 = new Pregunta();
        p1.setId(10L);
        Pregunta p2 = new Pregunta();
        p2.setId(11L);
        Pregunta p3 = new Pregunta();
        p3.setId(12L);
        combo.setPreguntas(new HashSet<>(Arrays.asList(
            preguntaCombo(combo, p1, 1, "2"),
            preguntaCombo(combo, p2, 2, "3"),
            preguntaCombo(combo, p3, 3, "X"))));
        jornada.setCombos(new HashSet<>(Collections.singletonList(combo)));
        when(jornadaRepository.findById(1L)).thenReturn(Optional.of(jornada));
        when(comboRepository.findById(3L)).thenReturn(Optional.of(combo));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> jornadaService.reciclarComboParcial(1L, 3L, 999L, 5L));
        assertTrue(ex.getMessage().contains("no pertenece"));
    }

    @Test
    void cancelarReciclajeCombo_registraUndo() {
        when(typedQuery.getSingleResult()).thenReturn(0L);
        Map<String, Object> histHijo = new HashMap<>();
        histHijo.put("id", 50L);
        histHijo.put("jornada_id", 1L);
        histHijo.put("notas", "RECICLAJE_PARCIAL_COMBO_HIJO;PADRE:3");
        Map<String, Object> histPadre = new HashMap<>();
        histPadre.put("id", 51L);
        histPadre.put("jornada_id", 1L);
        histPadre.put("notas", "RECICLAJE_PARCIAL_COMBO_PADRE:3");
        Map<String, Object> filaCombo = new HashMap<>();
        filaCombo.put("id", 9L);
        when(undoService.snapshotFilas(eq("historial_jornadas"), eq("combo_id"), eq(9L)))
                .thenReturn(Collections.singletonList(histHijo));
        when(undoService.snapshotFilas(eq("historial_jornadas"), eq("combo_id"), eq(3L)))
                .thenReturn(Collections.singletonList(histPadre));
        when(undoService.snapshotFila(eq("combos"), eq(9L))).thenReturn(filaCombo);
        when(undoService.snapshotFilas(eq("combos_preguntas"), eq("combo_id"), eq(9L)))
                .thenReturn(Collections.emptyList());
        when(nativeQuery.executeUpdate()).thenReturn(1);

        jornadaService.cancelarReciclajeCombo(1L, 9L);

        verify(comboRepository).deleteById(9L);
        verify(undoService).registrar(eq("cancelar_reciclaje_combo"), anyString(), any());
    }

    @Test
    void cancelarReciclajeCombo_asignadoLanza() {
        when(typedQuery.getSingleResult()).thenReturn(1L);
        assertThrows(IllegalStateException.class, () -> jornadaService.cancelarReciclajeCombo(1L, 9L));
    }

    @Test
    void cancelarReciclajeCombo_noDerivadoLanza() {
        when(typedQuery.getSingleResult()).thenReturn(0L);
        when(undoService.snapshotFilas(anyString(), anyString(), any())).thenReturn(Collections.emptyList());
        assertThrows(IllegalArgumentException.class, () -> jornadaService.cancelarReciclajeCombo(1L, 9L));
    }

    private PreguntaCombo preguntaCombo(Combo combo, Pregunta pregunta, int posicion, String factor) {
        PreguntaCombo.PreguntaComboId id = new PreguntaCombo.PreguntaComboId();
        id.setComboId(combo.getId());
        id.setPreguntaId(pregunta.getId());
        PreguntaCombo pc = new PreguntaCombo();
        pc.setId(id);
        pc.setCombo(combo);
        pc.setPregunta(pregunta);
        pc.setPosicion(posicion);
        pc.setFactorMultiplicacion(factor);
        return pc;
    }

    @Test
    void reutilizarCombo_adjudicado() {
        Jornada jornada = new Jornada();
        jornada.setId(1L);
        Combo combo = new Combo();
        combo.setId(4L);
        combo.setEstado(Combo.EstadoCombo.adjudicado);
        jornada.setCombos(new HashSet<>(Collections.singletonList(combo)));
        when(jornadaRepository.findById(1L)).thenReturn(Optional.of(jornada));
        when(comboRepository.findById(4L)).thenReturn(Optional.of(combo));
        when(comboService.cambiarEstadoAtomico(eq(4L),
            eq(Combo.EstadoCombo.adjudicado),
            eq(Combo.EstadoCombo.aprobado))).thenReturn(true);
        when(nativeQuery.getSingleResult()).thenReturn(0L);

        jornadaService.reutilizarCombo(1L, 4L, 4L);
        verify(comboService).cambiarEstadoAtomico(eq(4L),
            eq(Combo.EstadoCombo.adjudicado),
            eq(Combo.EstadoCombo.aprobado));
    }

    @Test
    void actualizar_camposYAsignaciones() {
        Jornada jornada = new Jornada();
        jornada.setId(1L);
        jornada.setEstado(Jornada.EstadoJornada.preparacion);
        Usuario usuario = new Usuario();
        usuario.setId(10L);
        usuario.setNombre("admin");
        jornada.setCreacionUsuario(usuario);
        jornada.setCuestionarios(new HashSet<>());
        jornada.setCombos(new HashSet<>());
        when(jornadaRepository.findById(1L)).thenReturn(Optional.of(jornada));
        when(jornadaRepository.save(any(Jornada.class))).thenAnswer(inv -> inv.getArgument(0));

        Cuestionario cuest = new Cuestionario();
        cuest.setId(2L);
        cuest.setEstado(Cuestionario.EstadoCuestionario.aprobado);
        cuest.setNivel(Cuestionario.NivelCuestionario.NORMAL);
        Combo combo = new Combo();
        combo.setId(4L);
        combo.setEstado(Combo.EstadoCombo.aprobado);
        combo.setNivel(Combo.NivelCombo.NORMAL);
        when(cuestionarioRepository.findById(2L)).thenReturn(Optional.of(cuest));
        when(comboRepository.findById(4L)).thenReturn(Optional.of(combo));
        when(cuestionarioService.cambiarEstadoAtomico(any(), any(), any())).thenReturn(true);
        when(comboService.cambiarEstadoAtomico(any(), any(), any())).thenReturn(true);

        JornadaDTO dto = new JornadaDTO();
        dto.setNombre("Nueva");
        dto.setFechaJornada(LocalDate.now());
        dto.setLugar("Set");
        dto.setNotas("n");
        dto.setCuestionarioIds(Collections.singletonList(2L));
        dto.setComboIds(Collections.singletonList(4L));

        JornadaDTO result = jornadaService.actualizar(1L, dto);
        assertEquals("Nueva", result.getNombre());
        verify(cuestionarioService).cambiarEstadoAtomico(eq(2L),
            eq(Cuestionario.EstadoCuestionario.aprobado),
            eq(Cuestionario.EstadoCuestionario.adjudicado));
        verify(comboService).cambiarEstadoAtomico(eq(4L),
            eq(Combo.EstadoCombo.aprobado),
            eq(Combo.EstadoCombo.adjudicado));
    }

    @Test
    void crear_asignaCuestionarioYComboComoAdjudicado() {
        when(jornadaRepository.existsByNombre("Jornada nueva")).thenReturn(false);
        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(usuarioMinimo()));
        Cuestionario cuest = new Cuestionario();
        cuest.setId(2L);
        cuest.setEstado(Cuestionario.EstadoCuestionario.aprobado);
        cuest.setNivel(Cuestionario.NivelCuestionario.NORMAL);
        Combo combo = new Combo();
        combo.setId(4L);
        combo.setEstado(Combo.EstadoCombo.aprobado);
        combo.setNivel(Combo.NivelCombo.NORMAL);
        when(cuestionarioRepository.findById(2L)).thenReturn(Optional.of(cuest));
        when(comboRepository.findById(4L)).thenReturn(Optional.of(combo));
        when(cuestionarioService.cambiarEstadoAtomico(eq(2L),
            eq(Cuestionario.EstadoCuestionario.aprobado),
            eq(Cuestionario.EstadoCuestionario.adjudicado))).thenReturn(true);
        when(comboService.cambiarEstadoAtomico(eq(4L),
            eq(Combo.EstadoCombo.aprobado),
            eq(Combo.EstadoCombo.adjudicado))).thenReturn(true);

        JornadaDTO dto = dtoMinimo();
        dto.setCuestionarioIds(Collections.singletonList(2L));
        dto.setComboIds(Collections.singletonList(4L));

        jornadaService.crear(dto, 10L);

        verify(cuestionarioService).cambiarEstadoAtomico(eq(2L),
            eq(Cuestionario.EstadoCuestionario.aprobado),
            eq(Cuestionario.EstadoCuestionario.adjudicado));
        verify(comboService).cambiarEstadoAtomico(eq(4L),
            eq(Combo.EstadoCombo.aprobado),
            eq(Combo.EstadoCombo.adjudicado));
    }

    @Test
    void actualizar_quitarGrabadoNoLoPasaAAprobado() {
        Jornada jornada = jornadaBase();
        Cuestionario grabado = cuestionarioAdjudicado(2L);
        grabado.setEstado(Cuestionario.EstadoCuestionario.grabado);
        Combo comboGrabado = comboAdjudicado(11L);
        comboGrabado.setEstado(Combo.EstadoCombo.grabado);
        Cuestionario seQueda = cuestionarioAdjudicado(1L);
        Combo comboSeQueda = comboAdjudicado(10L);
        jornada.reemplazarCuestionariosPorSlot(Arrays.asList(seQueda, grabado, null, null, null, null));
        jornada.reemplazarCombosPorSlot(Arrays.asList(comboSeQueda, comboGrabado, null, null, null, null));
        when(jornadaRepository.findById(1L)).thenReturn(Optional.of(jornada));
        when(cuestionarioRepository.findById(1L)).thenReturn(Optional.of(seQueda));
        when(comboRepository.findById(10L)).thenReturn(Optional.of(comboSeQueda));

        JornadaDTO dto = dtoMinimo();
        dto.setCuestionarioIds(Arrays.asList(1L, null, null, null, null, null));
        dto.setComboIds(Arrays.asList(10L, null, null, null, null, null));

        jornadaService.actualizar(1L, dto);

        assertEquals(Cuestionario.EstadoCuestionario.grabado, grabado.getEstado());
        assertEquals(Combo.EstadoCombo.grabado, comboGrabado.getEstado());
        verify(cuestionarioRepository, never()).save(grabado);
        verify(comboRepository, never()).save(comboGrabado);
    }

    @Test
    void actualizar_quitarHuecoMedioNoCompacta() {
        Jornada jornada = jornadaBase();
        Cuestionario c1 = cuestionarioAdjudicado(1L);
        Cuestionario c2 = cuestionarioAdjudicado(2L);
        Cuestionario c3 = cuestionarioAdjudicado(3L);
        Combo k1 = comboAdjudicado(10L);
        Combo k2 = comboAdjudicado(11L);
        Combo k3 = comboAdjudicado(12L);
        jornada.reemplazarCuestionariosPorSlot(Arrays.asList(c1, c2, c3, null, null, null));
        jornada.reemplazarCombosPorSlot(Arrays.asList(k1, k2, k3, null, null, null));
        when(jornadaRepository.findById(1L)).thenReturn(Optional.of(jornada));
        when(cuestionarioRepository.findById(1L)).thenReturn(Optional.of(c1));
        when(cuestionarioRepository.findById(3L)).thenReturn(Optional.of(c3));
        when(comboRepository.findById(10L)).thenReturn(Optional.of(k1));
        when(comboRepository.findById(12L)).thenReturn(Optional.of(k3));

        JornadaDTO dto = dtoMinimo();
        dto.setCuestionarioIds(Arrays.asList(1L, null, 3L, null, null, null));
        dto.setComboIds(Arrays.asList(10L, null, 12L, null, null, null));

        JornadaDTO result = jornadaService.actualizar(1L, dto);

        assertEquals(Arrays.asList(1L, null, 3L, null, null, null), result.getCuestionarioIds());
        assertEquals(Arrays.asList(10L, null, 12L, null, null, null), result.getComboIds());
        assertEquals(Cuestionario.EstadoCuestionario.aprobado, c2.getEstado());
        assertEquals(Combo.EstadoCombo.aprobado, k2.getEstado());
        assertNull(result.getCuestionarios().get(1));
        assertEquals(3L, result.getCuestionarios().get(2).getId());
        assertEquals(12L, result.getCombos().get(2).getId());
    }

    private Cuestionario cuestionarioAdjudicado(Long id) {
        Cuestionario c = new Cuestionario();
        c.setId(id);
        c.setEstado(Cuestionario.EstadoCuestionario.adjudicado);
        c.setNivel(Cuestionario.NivelCuestionario.NORMAL);
        return c;
    }

    private Combo comboAdjudicado(Long id) {
        Combo c = new Combo();
        c.setId(id);
        c.setEstado(Combo.EstadoCombo.adjudicado);
        c.setNivel(Combo.NivelCombo.NORMAL);
        return c;
    }

    @SuppressWarnings("unchecked")
    private List<String> deshacerAccionesConUndoReal(List<Map<String, Object>> acciones) {
        try {
            UndoService realUndo = new UndoService();
            JdbcTemplate jdbc = org.mockito.Mockito.mock(JdbcTemplate.class);
            AuthorizationService auth = org.mockito.Mockito.mock(AuthorizationService.class);
            ReflectionTestUtils.setField(realUndo, "jdbcTemplate", jdbc);
            ReflectionTestUtils.setField(realUndo, "authorizationService", auth);
            Usuario usuario = usuarioMinimo();
            when(auth.getCurrentUser()).thenReturn(Optional.of(usuario));
            String json = new ObjectMapper().writeValueAsString(Collections.singletonMap("acciones", acciones));
            Map<String, Object> operacion = new HashMap<>();
            operacion.put("usuario_id", usuario.getId());
            operacion.put("deshecha", 0);
            operacion.put("fecha_creacion", java.sql.Timestamp.from(java.time.Instant.now()));
            operacion.put("datos_undo", json);
            operacion.put("descripcion", "reciclaje");
            when(jdbc.queryForList(anyString(), eq(77L))).thenReturn(Collections.singletonList(operacion));
            List<String> sqls = new java.util.ArrayList<>();
            org.mockito.stubbing.Answer<Integer> recoger = inv -> {
                sqls.add(inv.getArgument(0));
                return 1;
            };
            org.mockito.Mockito.lenient().when(jdbc.update(anyString(), any(Object.class))).thenAnswer(recoger);
            org.mockito.Mockito.lenient().when(jdbc.update(anyString(), any(), any())).thenAnswer(recoger);
            org.mockito.Mockito.lenient().when(jdbc.update(anyString(), any(), any(), any())).thenAnswer(recoger);
            realUndo.deshacer(77L);
            return sqls;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
