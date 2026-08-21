package com.lsnls.service;

import com.lsnls.entity.Combo;
import com.lsnls.entity.Combo.EstadoCombo;
import com.lsnls.entity.Combo.NivelCombo;
import com.lsnls.entity.Pregunta;
import com.lsnls.entity.PreguntaCombo;
import com.lsnls.entity.Usuario;
import com.lsnls.repository.ComboRepository;
import com.lsnls.repository.PreguntaComboRepository;
import com.lsnls.repository.PreguntaRepository;
import com.lsnls.repository.TematicaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ComboServiceCoverageTest {

    @Mock private ComboRepository comboRepository;
    @Mock private PreguntaRepository preguntaRepository;
    @Mock private PreguntaComboRepository preguntaComboRepository;
    @Mock private TematicaRepository tematicaRepository;
    @Mock private EntityManager entityManager;
    @Mock private UndoService undoService;
    @Mock private TypedQuery<Object> typedQuery;
    @Mock private Query nativeQuery;

    @InjectMocks
    private ComboService comboService;

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
        when(nativeQuery.setFirstResult(anyInt())).thenReturn(nativeQuery);
        when(nativeQuery.setMaxResults(anyInt())).thenReturn(nativeQuery);
        when(nativeQuery.getSingleResult()).thenReturn(0L);
        when(nativeQuery.getResultList()).thenReturn(Collections.emptyList());
        when(nativeQuery.executeUpdate()).thenReturn(1);
    }

    private Combo comboConSlots() {
        Combo c = new Combo();
        c.setId(1L);
        c.setEstado(EstadoCombo.borrador);
        c.setNivel(NivelCombo.NORMAL);
        c.setTipo(Combo.TipoCombo.P);
        c.setTematica("Cine");
        Usuario u = new Usuario();
        u.setId(1L);
        u.setNombre("dir");
        c.setCreacionUsuario(u);

        HashSet<PreguntaCombo> set = new HashSet<>();
        set.add(pc(c, 11L, "X2", 1));
        set.add(pc(c, 12L, "X3", 2));
        set.add(pc(c, 13L, "X", 3));
        c.setPreguntas(set);
        return c;
    }

    private PreguntaCombo pc(Combo c, long preguntaId, String factor, Integer pos) {
        Pregunta p = new Pregunta();
        p.setId(preguntaId);
        p.setPregunta("P" + preguntaId);
        p.setRespuesta("R");
        p.setTematica("T");
        p.setNivel(Pregunta.NivelPregunta._5LS);
        p.setEstado(Pregunta.EstadoPregunta.aprobada);
        PreguntaCombo rel = new PreguntaCombo();
        rel.setCombo(c);
        rel.setPregunta(p);
        rel.setFactorMultiplicacion(factor);
        rel.setPosicion(pos);
        return rel;
    }

    @Test
    void obtenerComboConSlots_conPosicionesYJornada() {
        Combo c = comboConSlots();
        when(typedQuery.getResultList()).thenReturn(Collections.singletonList(c), Collections.singletonList(7L));

        Map<String, Object> dto = comboService.obtenerComboConSlots(1L);

        assertNotNull(dto);
        assertEquals(1L, dto.get("id"));
        assertEquals(7L, dto.get("jornadaAsignada"));
        assertNotNull(dto.get("preguntas"));
    }

    @Test
    void obtenerComboConSlots_legacyPorFactorYReutilizado() {
        Combo c = comboConSlots();
        for (PreguntaCombo rel : c.getPreguntas()) {
            rel.setPosicion(null);
        }
        when(typedQuery.getResultList()).thenReturn(Collections.singletonList(c));
        when(nativeQuery.getResultList()).thenReturn(Collections.singletonList(new Object[]{9L, "J1"}));

        Map<String, Object> dto = comboService.obtenerComboConSlots(1L);

        assertEquals(9L, dto.get("reutilizadoDeJornadaId"));
        assertEquals("J1", dto.get("reutilizadoDeJornadaNombre"));
    }

    @Test
    void obtenerComboConSlots_vacio() {
        assertNull(comboService.obtenerComboConSlots(99L));
    }

    @Test
    void filtrarCombos_porTextoConFiltros() {
        Combo c = comboConSlots();
        when(nativeQuery.getResultList()).thenReturn(Collections.singletonList(1L));
        when(nativeQuery.getSingleResult()).thenReturn(1L);
        when(comboRepository.findById(1L)).thenReturn(Optional.of(c));
        when(typedQuery.getResultList()).thenReturn(Collections.singletonList(c));

        Map<String, Object> res = comboService.filtrarCombos("borrador", "P", "Cine", "geo", "paris", 0, 10);

        assertEquals(1L, res.get("totalItems"));
        assertEquals(0, res.get("currentPage"));
    }

    @Test
    void filtrarCombos_porEstadoTipoTematica() {
        Combo c = comboConSlots();
        Page<Combo> page = new PageImpl<>(Collections.singletonList(c));
        when(comboRepository.findByEstadoAndTipoAndTematica(eq(EstadoCombo.borrador), eq(Combo.TipoCombo.P), eq("Cine"), any(Pageable.class)))
            .thenReturn(page);
        when(typedQuery.getResultList()).thenReturn(Collections.singletonList(c));

        Map<String, Object> res = comboService.filtrarCombos("borrador", "P", "Cine", null, null, 0, 10);
        assertEquals(1L, res.get("totalItems"));
    }

    @Test
    void filtrarCombos_ramasParcialesYSinFiltro() {
        Combo c = comboConSlots();
        Page<Combo> page = new PageImpl<>(Collections.singletonList(c));
        when(comboRepository.findByEstadoAndTipo(any(), any(), any(Pageable.class))).thenReturn(page);
        when(comboRepository.findByEstadoAndTematica(any(), any(), any(Pageable.class))).thenReturn(page);
        when(comboRepository.findByTipoAndTematica(any(), any(), any(Pageable.class))).thenReturn(page);
        when(comboRepository.findByEstado(any(EstadoCombo.class), any(Pageable.class))).thenReturn(page);
        when(comboRepository.findByTipo(any(), any(Pageable.class))).thenReturn(page);
        when(comboRepository.findByTematica(any(), any(Pageable.class))).thenReturn(page);
        when(comboRepository.count()).thenReturn(1L);
        when(comboRepository.findAllPaginados(any(Pageable.class))).thenReturn(Collections.singletonList(c));
        when(typedQuery.getResultList()).thenReturn(Collections.singletonList(c));

        assertNotNull(comboService.filtrarCombos("borrador", "P", null, null, null, 0, 5));
        assertNotNull(comboService.filtrarCombos("borrador", null, "Cine", null, null, 0, 5));
        assertNotNull(comboService.filtrarCombos(null, "P", "Cine", null, null, 0, 5));
        assertNotNull(comboService.filtrarCombos("borrador", null, null, null, null, 0, 5));
        assertNotNull(comboService.filtrarCombos(null, "P", null, null, null, 0, 5));
        assertNotNull(comboService.filtrarCombos(null, null, "Cine", null, null, 0, 5));
        assertNotNull(comboService.filtrarCombos(null, null, null, null, null, 0, 5));
    }

    @Test
    void filtrarCombosPorId_exactoYParcial() {
        Combo c = comboConSlots();
        when(comboRepository.findById(1L)).thenReturn(Optional.of(c));
        when(typedQuery.getResultList()).thenReturn(Collections.singletonList(c));
        Map<String, Object> exacto = comboService.filtrarCombosPorId("1", 0, 10);
        assertEquals(1, exacto.get("totalItems"));

        when(comboRepository.findByIdContaining(eq("ab"), any(Pageable.class)))
            .thenReturn(new PageImpl<>(Collections.singletonList(c)));
        Map<String, Object> parcial = comboService.filtrarCombosPorId("ab", 0, 10);
        assertEquals(1L, parcial.get("totalItems"));
    }

    @Test
    void obtenerTodosPaginados_yDisponibles() {
        Combo c = comboConSlots();
        when(comboRepository.count()).thenReturn(1L);
        when(comboRepository.findAllPaginados(any(Pageable.class))).thenReturn(Collections.singletonList(c));
        when(typedQuery.getResultList()).thenReturn(Collections.singletonList(c));
        when(comboRepository.findByEstado(EstadoCombo.aprobado)).thenReturn(Collections.singletonList(c));
        when(comboRepository.findByEstado(EstadoCombo.adjudicado)).thenReturn(Collections.emptyList());
        when(comboRepository.findByEstado(EstadoCombo.grabado)).thenReturn(Collections.emptyList());

        assertEquals(1L, comboService.obtenerTodosPaginados(0, 10).get("totalItems"));
        assertEquals(1, comboService.obtenerDisponiblesParaConcursantes().size());
    }

    @Test
    void validarCompletoParaAprobar_okYErrores() {
        Combo ok = comboConSlots();
        comboService.validarCompletoParaAprobar(ok);

        assertThrows(IllegalArgumentException.class, () -> comboService.validarCompletoParaAprobar(null));
        Combo vacio = comboConSlots();
        vacio.setPreguntas(new HashSet<>());
        assertThrows(IllegalArgumentException.class, () -> comboService.validarCompletoParaAprobar(vacio));
    }

    @Test
    void actualizarFactorPregunta_ramas() {
        assertFalse(comboService.actualizarFactorPregunta(1L, 2L, " "));
        Combo c = comboConSlots();
        when(comboRepository.findById(1L)).thenReturn(Optional.of(c));
        when(preguntaRepository.findById(11L)).thenReturn(Optional.empty());
        assertFalse(comboService.actualizarFactorPregunta(1L, 11L, "X2"));

        Pregunta p = new Pregunta();
        p.setId(11L);
        when(preguntaRepository.findById(11L)).thenReturn(Optional.of(p));
        when(preguntaComboRepository.findById(any())).thenReturn(Optional.empty());
        assertFalse(comboService.actualizarFactorPregunta(1L, 11L, "X3"));

        PreguntaCombo rel = new PreguntaCombo();
        when(preguntaComboRepository.findById(any())).thenReturn(Optional.of(rel));
        assertTrue(comboService.actualizarFactorPregunta(1L, 11L, "X"));
    }

    @Test
    void cambiarEstadoAtomico_okYConflicto() {
        when(nativeQuery.executeUpdate()).thenReturn(1);
        assertTrue(comboService.cambiarEstadoAtomico(1L, EstadoCombo.borrador, EstadoCombo.revisar));

        when(nativeQuery.executeUpdate()).thenReturn(0);
        assertThrows(IllegalStateException.class,
            () -> comboService.cambiarEstadoAtomico(1L, EstadoCombo.borrador, EstadoCombo.revisar));
    }

    @Test
    void actualizar_existeYNo() {
        Combo c = comboConSlots();
        when(comboRepository.existsById(1L)).thenReturn(true);
        when(comboRepository.save(any())).thenReturn(c);
        assertNotNull(comboService.actualizar(1L, c));
        when(comboRepository.existsById(9L)).thenReturn(false);
        assertNull(comboService.actualizar(9L, c));
    }
}
