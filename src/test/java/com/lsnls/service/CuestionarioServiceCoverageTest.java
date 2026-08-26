package com.lsnls.service;

import com.lsnls.dto.CrearCuestionarioDTO;
import com.lsnls.entity.Cuestionario;
import com.lsnls.entity.Cuestionario.EstadoCuestionario;
import com.lsnls.entity.Pregunta;
import com.lsnls.entity.PreguntaCuestionario;
import com.lsnls.entity.Usuario;
import com.lsnls.repository.CuestionarioRepository;
import com.lsnls.repository.PreguntaCuestionarioRepository;
import com.lsnls.repository.PreguntaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CuestionarioServiceCoverageTest {

    @Mock private CuestionarioRepository cuestionarioRepository;
    @Mock private PreguntaRepository preguntaRepository;
    @Mock private PreguntaCuestionarioRepository preguntaCuestionarioRepository;
    @Mock private EntityManager entityManager;
    @Mock private UndoService undoService;
    @Mock private TematicaService tematicaService;
    @Mock private TypedQuery<Object> typedQuery;
    @Mock private Query nativeQuery;

    @InjectMocks
    private CuestionarioService cuestionarioService;

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
        when(cuestionarioRepository.save(any(Cuestionario.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private Cuestionario conPreguntas() {
        Cuestionario c = new Cuestionario();
        c.setId(1L);
        c.setEstado(EstadoCuestionario.borrador);
        c.setTematica("Cine");
        HashSet<PreguntaCuestionario> set = new HashSet<>();
        set.add(pc(c, 1L, Pregunta.NivelPregunta._1LS));
        set.add(pc(c, 2L, Pregunta.NivelPregunta._2NLS));
        set.add(pc(c, 3L, Pregunta.NivelPregunta._3LS));
        set.add(pc(c, 4L, Pregunta.NivelPregunta._4NLS));
        c.setPreguntas(set);
        return c;
    }

    private PreguntaCuestionario pc(Cuestionario c, long id, Pregunta.NivelPregunta nivel) {
        Pregunta p = new Pregunta();
        p.setId(id);
        p.setNivel(nivel);
        p.setPregunta("P" + id);
        p.setRespuesta("R");
        p.setTematica("T");
        p.setEstado(Pregunta.EstadoPregunta.aprobada);
        PreguntaCuestionario rel = new PreguntaCuestionario();
        rel.setCuestionario(c);
        rel.setPregunta(p);
        rel.setFactorMultiplicacion(1);
        return rel;
    }

    @Test
    void obtenerCuestionarioConSlots_ok() {
        Cuestionario c = conPreguntas();
        when(typedQuery.getResultList()).thenReturn(Collections.singletonList(c), Collections.singletonList(8L));
        when(nativeQuery.getResultList()).thenReturn(Collections.singletonList(new Object[]{3L, "Jornada 1"}));

        Map<String, Object> dto = cuestionarioService.obtenerCuestionarioConSlots(1L);
        assertNotNull(dto);
        assertEquals(1L, dto.get("id"));
        assertEquals(8L, dto.get("jornadaAsignada"));
        assertEquals(3L, dto.get("reutilizadoDeJornadaId"));
    }

    @Test
    void filtrarCuestionarios_textoYRamas() {
        Cuestionario c = conPreguntas();
        when(nativeQuery.getResultList()).thenReturn(Collections.singletonList(1L));
        when(nativeQuery.getSingleResult()).thenReturn(1L);
        when(cuestionarioRepository.findById(1L)).thenReturn(Optional.of(c));
        when(typedQuery.getResultList()).thenReturn(Collections.singletonList(c));

        Map<String, Object> conTexto = cuestionarioService.filtrarCuestionarios("borrador", "Cine", "geo", "paris", 0, 10);
        assertEquals(1L, conTexto.get("totalItems"));

        when(cuestionarioRepository.findByEstadoAndTematicaContainingIgnoreCase(any(), any(), any(Pageable.class)))
            .thenReturn(new PageImpl<>(Collections.singletonList(c)));
        when(cuestionarioRepository.findByEstado(any(EstadoCuestionario.class), any(Pageable.class)))
            .thenReturn(new PageImpl<>(Collections.singletonList(c)));
        when(cuestionarioRepository.findByTematicaContainingIgnoreCase(any(), any(Pageable.class)))
            .thenReturn(new PageImpl<>(Collections.singletonList(c)));
        when(cuestionarioRepository.count()).thenReturn(1L);
        when(cuestionarioRepository.findAllPaginados(any(Pageable.class))).thenReturn(Collections.singletonList(c));

        assertNotNull(cuestionarioService.filtrarCuestionarios("borrador", "Cine", null, null, 0, 5));
        assertNotNull(cuestionarioService.filtrarCuestionarios("borrador", null, null, null, 0, 5));
        assertNotNull(cuestionarioService.filtrarCuestionarios(null, "Cine", null, null, 0, 5));
        assertNotNull(cuestionarioService.filtrarCuestionarios(null, null, null, null, 0, 5));
    }

    @Test
    void filtrarPorId_exactoNoEncontradoYParcial() {
        Cuestionario c = conPreguntas();
        when(cuestionarioRepository.findById(1L)).thenReturn(Optional.of(c));
        when(typedQuery.getResultList()).thenReturn(Collections.singletonList(c));
        assertEquals(1, cuestionarioService.filtrarCuestionariosPorId("1", 0, 10).get("totalItems"));

        when(cuestionarioRepository.findById(9L)).thenReturn(Optional.empty());
        assertEquals(0, cuestionarioService.filtrarCuestionariosPorId("9", 0, 10).get("totalItems"));

        when(cuestionarioRepository.findByIdContaining(eq("ab"), any(Pageable.class)))
            .thenReturn(new PageImpl<>(Collections.singletonList(c)));
        assertEquals(1L, cuestionarioService.filtrarCuestionariosPorId("ab", 0, 10).get("totalItems"));
    }

    @Test
    void validarCompletoYTematicaYPaginado() {
        cuestionarioService.validarCompletoParaAprobar(conPreguntas());
        assertThrows(IllegalArgumentException.class, () -> cuestionarioService.validarCompletoParaAprobar(null));

        Cuestionario c = conPreguntas();
        when(cuestionarioRepository.findById(1L)).thenReturn(Optional.of(c));
        assertEquals("Nueva", cuestionarioService.cambiarTematica(1L, "Nueva").getTematica());
        when(cuestionarioRepository.findById(9L)).thenReturn(Optional.empty());
        assertNull(cuestionarioService.cambiarTematica(9L, "X"));

        when(tematicaService.obtenerNombresTematicas()).thenReturn(Collections.singletonList("Cine"));
        assertEquals(1, cuestionarioService.obtenerTematicasDisponibles().size());

        when(cuestionarioRepository.count()).thenReturn(1L);
        when(cuestionarioRepository.findAllPaginados(any(Pageable.class))).thenReturn(Collections.singletonList(c));
        when(typedQuery.getResultList()).thenReturn(Collections.singletonList(c));
        assertEquals(1L, cuestionarioService.obtenerTodosPaginados(0, 10).get("totalItems"));
        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(entityManager, atLeastOnce()).createNativeQuery(sql.capture());
        assertTrue(sql.getAllValues().stream().anyMatch(s ->
                s.contains("SET estado='adjudicado'") && s.contains("jornadas_cuestionarios")));
        assertTrue(sql.getAllValues().stream().anyMatch(s ->
                s.contains("SET estado='aprobado'") && s.contains("NOT IN (SELECT cuestionario_id FROM jornadas_cuestionarios)")));
    }

    @Test
    void actualizarDesdeDTO_cambiaTematicaYNotas() {
        Cuestionario c = conPreguntas();
        when(cuestionarioRepository.findById(1L)).thenReturn(Optional.of(c));
        CrearCuestionarioDTO dto = new CrearCuestionarioDTO();
        dto.setTematica("Historia");
        dto.setNotasDireccion("n");
        dto.setVersion(2L);
        dto.setPreguntasNormales(Arrays.asList(1L, 2L, 3L, 4L));

        Cuestionario saved = cuestionarioService.actualizarDesdeDTO(1L, dto);
        assertEquals("Historia", saved.getTematica());
        assertEquals("n", saved.getNotasDireccion());
    }

    @Test
    void cambiarEstadoAtomico() {
        when(nativeQuery.executeUpdate()).thenReturn(1);
        assertTrue(cuestionarioService.cambiarEstadoAtomico(1L, EstadoCuestionario.borrador, EstadoCuestionario.revisar));
        when(nativeQuery.executeUpdate()).thenReturn(0);
        assertThrows(IllegalStateException.class,
            () -> cuestionarioService.cambiarEstadoAtomico(1L, EstadoCuestionario.borrador, EstadoCuestionario.revisar));
    }
}
