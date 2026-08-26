package com.lsnls.service;

import com.lsnls.dto.ConcursanteDTO;
import com.lsnls.entity.Combo;
import com.lsnls.entity.Concursante;
import com.lsnls.entity.Cuestionario;
import com.lsnls.entity.Jornada;
import com.lsnls.entity.Usuario;
import com.lsnls.repository.ComboRepository;
import com.lsnls.repository.ConcursanteRepository;
import com.lsnls.repository.CuestionarioRepository;
import com.lsnls.repository.JornadaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ConcursanteServiceTest {

    @Mock private ConcursanteRepository concursanteRepository;
    @Mock private CuestionarioRepository cuestionarioRepository;
    @Mock private ComboRepository comboRepository;
    @Mock private JornadaRepository jornadaRepository;
    @Mock private EntityManager entityManager;
    @Mock private CuestionarioService cuestionarioService;
    @Mock private ComboService comboService;
    @Mock private JornadaService jornadaService;
    @Mock private UndoService undoService;
    @Mock private TypedQuery<Object> typedQuery;
    @Mock private Query nativeQuery;

    @InjectMocks
    private ConcursanteService concursanteService;

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
        when(jornadaService.esComboDerivado(any())).thenReturn(false);
        when(concursanteRepository.save(any(Concursante.class))).thenAnswer(inv -> {
            Concursante c = inv.getArgument(0);
            if (c.getId() == null) {
                c.setId(1L);
            }
            return c;
        });
    }

    private Concursante concursanteBase() {
        Concursante c = new Concursante();
        c.setId(1L);
        c.setNombre("Ana");
        c.setEstado("grabado");
        c.setNumeroConcursante(7);
        return c;
    }

    private ConcursanteDTO dtoMinimo() {
        ConcursanteDTO dto = new ConcursanteDTO();
        dto.setNombre("Ana");
        dto.setEstado("grabado");
        return dto;
    }

    @Test
    void findAll() {
        when(concursanteRepository.findAll()).thenReturn(Collections.singletonList(concursanteBase()));
        List<ConcursanteDTO> result = concursanteService.findAll();
        assertEquals(1, result.size());
        assertEquals("Ana", result.get(0).getNombre());
    }

    @Test
    void findById_nullSiNoExiste() {
        when(concursanteRepository.findById(9L)).thenReturn(Optional.empty());
        assertNull(concursanteService.findById(9L));
    }

    @Test
    void findById_ok() {
        when(concursanteRepository.findById(1L)).thenReturn(Optional.of(concursanteBase()));
        ConcursanteDTO dto = concursanteService.findById(1L);
        assertEquals(1L, dto.getId());
        assertEquals("Ana", dto.getNombre());
    }

    @Test
    void create_okGeneraNumero() {
        when(concursanteRepository.findMaxNumeroConcursante()).thenReturn(4);
        ConcursanteDTO result = concursanteService.create(dtoMinimo());
        assertEquals("Ana", result.getNombre());
        assertEquals(5, result.getNumeroConcursante());
        assertEquals("grabado", result.getEstado());
        verify(concursanteRepository).save(any(Concursante.class));
    }

    @Test
    void create_sinEstadoOBorradorQuedaGrabado() {
        ConcursanteDTO dto = dtoMinimo();
        dto.setEstado(null);
        ConcursanteDTO sinEstado = concursanteService.create(dto);
        assertEquals("grabado", sinEstado.getEstado());

        dto.setEstado("borrador");
        ConcursanteDTO desdeBorrador = concursanteService.create(dto);
        assertEquals("grabado", desdeBorrador.getEstado());
    }

    @Test
    void create_primerNumeroSiNoHayMax() {
        when(concursanteRepository.findMaxNumeroConcursante()).thenReturn(null);
        ConcursanteDTO result = concursanteService.create(dtoMinimo());
        assertEquals(1, result.getNumeroConcursante());
    }

    @Test
    void create_duracionInvalida() {
        ConcursanteDTO dto = dtoMinimo();
        dto.setDuracion("99");
        assertThrows(IllegalArgumentException.class, () -> concursanteService.create(dto));
    }

    @Test
    void create_editadoSinDuracionFalla() {
        ConcursanteDTO dto = dtoMinimo();
        dto.setEstado("editado");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> concursanteService.create(dto));
        assertTrue(ex.getMessage().contains("editado"));
    }

    @Test
    void update_noEncontrado() {
        when(concursanteRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> concursanteService.update(1L, dtoMinimo()));
    }

    @Test
    void update_okCambiaNombre() {
        Concursante existente = concursanteBase();
        when(concursanteRepository.findById(1L)).thenReturn(Optional.of(existente));
        ConcursanteDTO dto = dtoMinimo();
        dto.setNombre("Luis");
        dto.setEstado("grabado");

        ConcursanteDTO result = concursanteService.update(1L, dto);

        assertEquals("grabado", result.getEstado());
        verify(concursanteRepository).save(existente);
    }

    @Test
    void update_borradorEnDtoQuedaGrabado() {
        Concursante existente = concursanteBase();
        when(concursanteRepository.findById(1L)).thenReturn(Optional.of(existente));
        ConcursanteDTO dto = dtoMinimo();
        dto.setEstado("borrador");

        ConcursanteDTO result = concursanteService.update(1L, dto);

        assertEquals("grabado", result.getEstado());
    }

    @Test
    void delete_noExiste() {
        when(concursanteRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> concursanteService.delete(1L));
    }

    @Test
    void delete_asignadoAPrograma() {
        Concursante c = concursanteBase();
        c.setNumeroPrograma(12);
        when(concursanteRepository.findById(1L)).thenReturn(Optional.of(c));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> concursanteService.delete(1L));
        assertTrue(ex.getMessage().contains("programa"));
    }

    @Test
    void delete_grabadoPermitido() {
        Concursante c = concursanteBase();
        c.setEstado("grabado");
        when(concursanteRepository.findById(1L)).thenReturn(Optional.of(c));
        concursanteService.delete(1L);
        verify(concursanteRepository).deleteById(1L);
    }

    @Test
    void delete_okRestauraCuestionarioYCombo() {
        Concursante c = concursanteBase();
        Cuestionario q = new Cuestionario();
        q.setId(20L);
        q.setEstado(Cuestionario.EstadoCuestionario.grabado);
        Combo combo = new Combo();
        combo.setId(30L);
        combo.setEstado(Combo.EstadoCombo.grabado);
        c.setCuestionario(q);
        c.setCombo(combo);
        when(concursanteRepository.findById(1L)).thenReturn(Optional.of(c));

        concursanteService.delete(1L);

        verify(concursanteRepository).deleteById(1L);
        verify(undoService).snapshotFila("concursantes", 1L);
        verify(undoService).registrar(eq("eliminar_concursante"), anyString(), any());
        assertEquals(Cuestionario.EstadoCuestionario.aprobado, q.getEstado());
        assertEquals(Combo.EstadoCombo.aprobado, combo.getEstado());
    }

    @Test
    void asignarAPrograma_soloEditado() {
        Concursante c = concursanteBase();
        c.setEstado("grabado");
        when(concursanteRepository.findById(1L)).thenReturn(Optional.of(c));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> concursanteService.asignarAPrograma(1L, 8L));
        assertTrue(ex.getMessage().contains("editado"));
    }

    @Test
    void asignarAPrograma_grabadoFalla() {
        Concursante c = concursanteBase();
        c.setEstado("grabado");
        when(concursanteRepository.findById(1L)).thenReturn(Optional.of(c));
        assertThrows(IllegalArgumentException.class, () -> concursanteService.asignarAPrograma(1L, 8L));
    }

    @Test
    void asignarAPrograma_okSinPosicion() {
        Concursante c = concursanteBase();
        c.setEstado("editado");
        c.setDuracion("12:30");
        c.setNumeroPrograma(null);
        when(concursanteRepository.findById(1L)).thenReturn(Optional.of(c));
        when(concursanteRepository.findByNumeroProgramaOrderByNumeroConcursanteAsc(8))
            .thenReturn(Collections.emptyList());

        ConcursanteDTO result = concursanteService.asignarAPrograma(1L, 8L);

        assertEquals(8, result.getNumeroPrograma());
        assertEquals(1, result.getNumeroConcursante());
    }

    @Test
    void asignarAPrograma_posicionOcupada() {
        Concursante c = concursanteBase();
        c.setEstado("EDITADO");
        when(concursanteRepository.findById(1L)).thenReturn(Optional.of(c));
        when(concursanteRepository.countByNumeroProgramaAndNumeroConcursante(8, 2)).thenReturn(1L);

        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> concursanteService.asignarAPrograma(1L, 8L, 2));
        assertTrue(ex.getMessage().contains("ocupada"));
    }

    @Test
    void asignarAPrograma_posicionInvalida() {
        Concursante c = concursanteBase();
        c.setEstado("editado");
        when(concursanteRepository.findById(1L)).thenReturn(Optional.of(c));
        assertThrows(RuntimeException.class, () -> concursanteService.asignarAPrograma(1L, 8L, 4));
    }

    @Test
    void asignarAPrograma_programaLleno() {
        Concursante c = concursanteBase();
        c.setEstado("editado");
        when(concursanteRepository.findById(1L)).thenReturn(Optional.of(c));
        Concursante a = new Concursante();
        a.setNumeroConcursante(1);
        Concursante b = new Concursante();
        b.setNumeroConcursante(2);
        Concursante d = new Concursante();
        d.setNumeroConcursante(3);
        when(concursanteRepository.findByNumeroProgramaOrderByNumeroConcursanteAsc(8))
            .thenReturn(Arrays.asList(a, b, d));

        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> concursanteService.asignarAPrograma(1L, 8L));
        assertTrue(ex.getMessage().contains("3 posiciones"));
    }

    @Test
    void asignarAPrograma_noEncontrado() {
        when(concursanteRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> concursanteService.asignarAPrograma(1L, 8L));
    }

    @Test
    void desasignarDePrograma() {
        Concursante c = concursanteBase();
        c.setNumeroPrograma(8);
        when(concursanteRepository.findById(1L)).thenReturn(Optional.of(c));

        ConcursanteDTO result = concursanteService.desasignarDePrograma(1L);

        assertNull(result.getNumeroPrograma());
    }

    @Test
    void desasignarDePrograma_noEncontrado() {
        when(concursanteRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> concursanteService.desasignarDePrograma(1L));
    }

    @Test
    void asignarAJornada_ok() {
        Concursante c = concursanteBase();
        Jornada j = new Jornada();
        j.setId(3L);
        j.setNombre("J3");
        j.setEstado(Jornada.EstadoJornada.preparacion);
        j.setCreacionUsuario(new Usuario());
        j.getCreacionUsuario().setId(1L);
        j.getCreacionUsuario().setNombre("admin");
        j.getCreacionUsuario().setRol(Usuario.RolUsuario.ROLE_ADMIN);
        when(concursanteRepository.findById(1L)).thenReturn(Optional.of(c));
        when(jornadaRepository.findById(3L)).thenReturn(Optional.of(j));

        ConcursanteDTO result = concursanteService.asignarAJornada(1L, 3L);

        assertEquals(3L, result.getJornadaId());
        assertEquals("J3", result.getJornadaNombre());
    }

    @Test
    void asignarAJornada_jornadaNoExiste() {
        when(concursanteRepository.findById(1L)).thenReturn(Optional.of(concursanteBase()));
        when(jornadaRepository.findById(3L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> concursanteService.asignarAJornada(1L, 3L));
    }

    @Test
    void asignarAJornada_noPuedeSiTieneCuestionario() {
        Concursante c = concursanteBase();
        Jornada actual = new Jornada();
        actual.setId(2L);
        c.setJornada(actual);
        Cuestionario q = new Cuestionario();
        q.setId(20L);
        c.setCuestionario(q);
        Jornada nueva = new Jornada();
        nueva.setId(3L);
        when(concursanteRepository.findById(1L)).thenReturn(Optional.of(c));
        when(jornadaRepository.findById(3L)).thenReturn(Optional.of(nueva));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> concursanteService.asignarAJornada(1L, 3L));
        assertTrue(ex.getMessage().contains("cuestionario o combo"));
    }

    @Test
    void desasignarDeJornada_ok() {
        Concursante c = concursanteBase();
        Jornada j = new Jornada();
        j.setId(3L);
        c.setJornada(j);
        when(concursanteRepository.findById(1L)).thenReturn(Optional.of(c));

        ConcursanteDTO result = concursanteService.desasignarDeJornada(1L);
        assertNull(result.getJornadaId());
    }

    @Test
    void desasignarDeJornada_conComboFalla() {
        Concursante c = concursanteBase();
        Combo combo = new Combo();
        combo.setId(9L);
        c.setCombo(combo);
        when(concursanteRepository.findById(1L)).thenReturn(Optional.of(c));
        assertThrows(IllegalArgumentException.class, () -> concursanteService.desasignarDeJornada(1L));
    }

    @Test
    void updateCampo_variosCampos() {
        Concursante c = concursanteBase();
        when(concursanteRepository.findById(1L)).thenReturn(Optional.of(c));
        Map<String, Object> campos = new HashMap<>();
        campos.put("resultado", "12");
        campos.put("estado", "grabado");
        campos.put("duracion", "10:05");
        campos.put("premio", "1500.50");
        campos.put("foto", "a.jpg");
        campos.put("momentosDestacados", "ok");
        campos.put("factorX", "x");
        campos.put("valoracionFinal", "alta");
        campos.put("creditosEspeciales", "cred");
        campos.put("xusoker", "si");

        ConcursanteDTO result = concursanteService.updateCampo(1L, campos);

        assertEquals(12, result.getResultado());
        assertEquals("grabado", result.getEstado());
        assertEquals("10:05", result.getDuracion());
        assertEquals(new BigDecimal("1500.50"), result.getPremio());
        assertEquals("a.jpg", result.getFoto());
    }

    @Test
    void updateCampo_resultadoNoNumerico() {
        when(concursanteRepository.findById(1L)).thenReturn(Optional.of(concursanteBase()));
        Map<String, Object> campos = new HashMap<>();
        campos.put("resultado", "x");
        assertThrows(RuntimeException.class, () -> concursanteService.updateCampo(1L, campos));
    }

    @Test
    void updateCampo_editadoSinDuracion() {
        when(concursanteRepository.findById(1L)).thenReturn(Optional.of(concursanteBase()));
        Map<String, Object> campos = new HashMap<>();
        campos.put("estado", "editado");
        assertThrows(IllegalArgumentException.class, () -> concursanteService.updateCampo(1L, campos));
    }

    @Test
    void updateCampo_borradorSeNormalizaAGrabado() {
        Concursante c = concursanteBase();
        when(concursanteRepository.findById(1L)).thenReturn(Optional.of(c));
        Map<String, Object> campos = new HashMap<>();
        campos.put("estado", "borrador");

        ConcursanteDTO result = concursanteService.updateCampo(1L, campos);

        assertEquals("grabado", result.getEstado());
    }

    @Test
    void asignarAPrograma_programadoFalla() {
        Concursante c = concursanteBase();
        c.setEstado("programado");
        c.setDuracion("12:00");
        when(concursanteRepository.findById(1L)).thenReturn(Optional.of(c));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> concursanteService.asignarAPrograma(1L, 8L));
        assertTrue(ex.getMessage().contains("editado"));
    }

    @Test
    void findByEstado() {
        when(concursanteRepository.findByEstado("grabado")).thenReturn(Collections.singletonList(concursanteBase()));
        assertEquals(1, concursanteService.findByEstado("grabado").size());
    }

    @Test
    void findConcursantesSinPrograma() {
        when(concursanteRepository.findByNumeroProgramaIsNull()).thenReturn(Collections.singletonList(concursanteBase()));
        List<ConcursanteDTO> result = concursanteService.findConcursantesSinPrograma();
        assertEquals(1, result.size());
        assertEquals("Ana", result.get(0).getNombre());
    }

    @Test
    void create_conCuestionarioYComboAprobados() {
        Cuestionario cuest = new Cuestionario();
        cuest.setId(10L);
        cuest.setEstado(Cuestionario.EstadoCuestionario.aprobado);
        Combo combo = new Combo();
        combo.setId(20L);
        combo.setEstado(Combo.EstadoCombo.aprobado);
        when(cuestionarioRepository.findById(10L)).thenReturn(Optional.of(cuest));
        when(comboRepository.findById(20L)).thenReturn(Optional.of(combo));
        when(concursanteRepository.findMaxNumeroConcursante()).thenReturn(1);
        when(typedQuery.getResultList()).thenReturn(Collections.emptyList());

        ConcursanteDTO dto = dtoMinimo();
        dto.setCuestionarioId(10L);
        dto.setComboId(20L);
        ConcursanteDTO result = concursanteService.create(dto);
        assertEquals("Ana", result.getNombre());
        verify(cuestionarioRepository).save(cuest);
        verify(comboRepository).save(combo);
    }

    @Test
    void create_cuestionarioYaAsignado() {
        Cuestionario cuest = new Cuestionario();
        cuest.setId(10L);
        cuest.setEstado(Cuestionario.EstadoCuestionario.aprobado);
        when(cuestionarioRepository.findById(10L)).thenReturn(Optional.of(cuest));
        Concursante otro = concursanteBase();
        otro.setNombre("Luis");
        when(typedQuery.getResultList()).thenReturn(Collections.singletonList(otro));
        ConcursanteDTO dto = dtoMinimo();
        dto.setCuestionarioId(10L);
        assertThrows(RuntimeException.class, () -> concursanteService.create(dto));
    }

    @Test
    void update_asignaCuestionarioYComboNuevos() {
        Concursante existente = concursanteBase();
        Cuestionario anterior = new Cuestionario();
        anterior.setId(1L);
        anterior.setEstado(Cuestionario.EstadoCuestionario.grabado);
        existente.setCuestionario(anterior);

        Cuestionario nuevo = new Cuestionario();
        nuevo.setId(11L);
        nuevo.setEstado(Cuestionario.EstadoCuestionario.aprobado);
        Combo comboNuevo = new Combo();
        comboNuevo.setId(21L);
        comboNuevo.setEstado(Combo.EstadoCombo.aprobado);

        when(concursanteRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(cuestionarioRepository.findById(11L)).thenReturn(Optional.of(nuevo));
        when(comboRepository.findById(21L)).thenReturn(Optional.of(comboNuevo));
        when(typedQuery.getResultList()).thenReturn(Collections.emptyList());

        ConcursanteDTO dto = dtoMinimo();
        dto.setVersion(2L);
        dto.setCuestionarioId(11L);
        dto.setComboId(21L);
        dto.setDuracion("12:30");

        ConcursanteDTO result = concursanteService.update(1L, dto);
        assertEquals("Ana", result.getNombre());
        verify(cuestionarioRepository).save(nuevo);
        verify(comboRepository).save(comboNuevo);
    }

    @Test
    void findById_conJornadaYComboReciclado() {
        Concursante c = concursanteBase();
        Jornada j = new Jornada();
        j.setId(3L);
        j.setNombre("J1");
        c.setJornada(j);
        Combo combo = new Combo();
        combo.setId(8L);
        c.setCombo(combo);
        when(concursanteRepository.findById(1L)).thenReturn(Optional.of(c));
        when(nativeQuery.getSingleResult()).thenReturn(2L);

        ConcursanteDTO dto = concursanteService.findById(1L);
        assertEquals(3L, dto.getJornadaId());
        assertEquals("J1", dto.getJornadaNombre());
        assertEquals(8L, dto.getComboId());
        assertTrue(Boolean.TRUE.equals(dto.getComboReciclado()));
    }
}
