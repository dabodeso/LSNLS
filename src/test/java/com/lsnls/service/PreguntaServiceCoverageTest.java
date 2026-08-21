package com.lsnls.service;

import com.lsnls.dto.PreguntaDTO;
import com.lsnls.entity.Pregunta;
import com.lsnls.entity.Pregunta.EstadoDisponibilidad;
import com.lsnls.entity.Pregunta.EstadoPregunta;
import com.lsnls.entity.Pregunta.FactorPregunta;
import com.lsnls.entity.Pregunta.NivelPregunta;
import com.lsnls.entity.Usuario;
import com.lsnls.repository.PreguntaComboRepository;
import com.lsnls.repository.PreguntaRepository;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PreguntaServiceCoverageTest {

    @Mock private PreguntaRepository preguntaRepository;
    @Mock private DataTransformationService dataTransformationService;
    @Mock private PreguntaComboRepository preguntaComboRepository;
    @Mock private UndoService undoService;
    @Mock private EntityManager entityManager;
    @Mock private UsuarioService usuarioService;
    @Mock private TypedQuery<Object> typedQuery;
    @Mock private Query nativeQuery;

    @InjectMocks
    private PreguntaService preguntaService;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        SecurityContextHolder.clearContext();
        when(entityManager.createQuery(anyString(), any(Class.class))).thenReturn((TypedQuery) typedQuery);
        when(entityManager.createQuery(anyString())).thenReturn(typedQuery);
        when(typedQuery.setParameter(anyString(), nullable(Object.class))).thenReturn(typedQuery);
        when(typedQuery.setParameter(anyInt(), nullable(Object.class))).thenReturn(typedQuery);
        when(typedQuery.getSingleResult()).thenReturn(0L);
        when(typedQuery.getResultList()).thenReturn(Collections.emptyList());
        when(entityManager.createNativeQuery(anyString())).thenReturn(nativeQuery);
        when(nativeQuery.setParameter(anyString(), nullable(Object.class))).thenReturn(nativeQuery);
        when(nativeQuery.setParameter(anyInt(), nullable(Object.class))).thenReturn(nativeQuery);
        when(nativeQuery.getSingleResult()).thenReturn(10L);
        when(nativeQuery.getResultList()).thenReturn(Collections.emptyList());
        when(nativeQuery.executeUpdate()).thenReturn(1);
        when(dataTransformationService.normalizarPregunta(nullable(String.class))).thenAnswer(inv -> inv.getArgument(0));
        when(dataTransformationService.normalizarRespuesta(nullable(String.class))).thenAnswer(inv -> inv.getArgument(0));
        when(dataTransformationService.normalizarTematica(nullable(String.class))).thenAnswer(inv -> inv.getArgument(0));
        when(dataTransformationService.validarPreguntaCompleta(nullable(String.class), nullable(String.class), nullable(String.class)))
            .thenReturn(new DataTransformationService.ValidationResult());
        when(undoService.snapshotFila(anyString(), any())).thenReturn(Collections.singletonMap("id", 1L));
        when(preguntaRepository.save(any(Pregunta.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private Pregunta preguntaBase() {
        Pregunta p = new Pregunta();
        p.setId(1L);
        p.setPregunta("Capital de Francia");
        p.setRespuesta("PARIS");
        p.setTematica("GEOGRAFIA");
        p.setFuentes("atlas");
        p.setNivel(NivelPregunta._1LS);
        p.setEstado(EstadoPregunta.borrador);
        p.setEstadoDisponibilidad(EstadoDisponibilidad.disponible);
        Usuario u = new Usuario();
        u.setId(3L);
        u.setNombre("guion");
        p.setCreacionUsuario(u);
        return p;
    }

    private PreguntaDTO dtoCompleto() {
        PreguntaDTO dto = new PreguntaDTO();
        dto.setId(99L);
        dto.setNivel(NivelPregunta._2NLS);
        dto.setTematica("CINE");
        dto.setPregunta("Quien gano el oscar");
        dto.setRespuesta("FORD");
        dto.setEstado("borrador");
        dto.setCreacionUsuarioId(3L);
        dto.setFechaCreacion("2026-01-01T10:00:00");
        dto.setVersion(0L);
        dto.setFuentes("wiki");
        dto.setFactor(FactorPregunta.X);
        return dto;
    }

    @Test
    void restaurarDesdeSnapshot_okYValidaciones() {
        assertThrows(IllegalArgumentException.class, () -> preguntaService.restaurarDesdeSnapshot(null));
        PreguntaDTO sinId = dtoCompleto();
        sinId.setId(null);
        assertThrows(IllegalArgumentException.class, () -> preguntaService.restaurarDesdeSnapshot(sinId));

        when(preguntaRepository.existsById(99L)).thenReturn(true);
        assertThrows(IllegalArgumentException.class, () -> preguntaService.restaurarDesdeSnapshot(dtoCompleto()));

        when(preguntaRepository.existsById(99L)).thenReturn(false);
        Pregunta restaurada = preguntaBase();
        restaurada.setId(99L);
        when(preguntaRepository.findById(99L)).thenReturn(Optional.of(restaurada));

        Pregunta result = preguntaService.restaurarDesdeSnapshot(dtoCompleto());
        assertEquals(99L, result.getId());
    }

    @Test
    void actualizarDesdeDTO_camposYVerificada() {
        Pregunta p = preguntaBase();
        when(preguntaRepository.findById(1L)).thenReturn(Optional.of(p));
        when(preguntaComboRepository.existsByPreguntaId(1L)).thenReturn(false);

        PreguntaDTO dto = new PreguntaDTO();
        dto.setVersion(2L);
        dto.setEstado("verificada");
        dto.setTematica("HISTORIA");
        dto.setPregunta("Nueva pregunta larga");
        dto.setRespuesta("RESP");
        dto.setNivel(NivelPregunta._1LS);
        dto.setNotas("n");
        dto.setFactor(FactorPregunta.X2);
        dto.setSubtema("S");
        dto.setCreacionUsuarioId(3L);

        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken("guion", "x"));

        Pregunta saved = preguntaService.actualizarDesdeDTO(1L, dto);
        assertEquals(EstadoPregunta.verificada, saved.getEstado());
        assertEquals("HISTORIA", saved.getTematica());
        SecurityContextHolder.clearContext();
    }

    @Test
    void actualizarDesdeDTO_noEncontradaYAutoría() {
        when(preguntaRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> preguntaService.actualizarDesdeDTO(1L, new PreguntaDTO()));

        Pregunta p = preguntaBase();
        when(preguntaRepository.findById(1L)).thenReturn(Optional.of(p));
        PreguntaDTO dto = new PreguntaDTO();
        dto.setCreacionUsuarioId(99L);
        assertThrows(IllegalArgumentException.class, () -> preguntaService.actualizarDesdeDTO(1L, dto));
    }

    @Test
    void cambiarEstadoAtomico_verificadaYConflicto() {
        Usuario u = new Usuario();
        u.setId(3L);
        u.setNombre("dir");
        when(nativeQuery.getSingleResult()).thenReturn("otro");
        when(nativeQuery.executeUpdate()).thenReturn(1);
        assertTrue(preguntaService.cambiarEstadoAtomico(1L, EstadoPregunta.borrador, EstadoPregunta.verificada, u));

        when(nativeQuery.executeUpdate()).thenReturn(0);
        assertThrows(IllegalStateException.class,
            () -> preguntaService.cambiarEstadoAtomico(1L, EstadoPregunta.borrador, EstadoPregunta.aprobada, u));
    }

    @Test
    void buscarYFiltrarPaginado() {
        Pregunta p = preguntaBase();
        Page<Pregunta> page = new PageImpl<>(Collections.singletonList(p));
        when(preguntaRepository.buscarPreguntasSinFiltroDisponibilidad(
            any(), any(), any(), any(), any(), any(), any(), any(Pageable.class))).thenReturn(page);
        when(preguntaRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(preguntaRepository.findAll(any(Pageable.class))).thenReturn(page);

        Pageable pageable = PageRequest.of(0, 10);
        assertEquals(1, preguntaService.buscarPreguntasPaginadas("1LS", "X", "1", "cap", "par", "geo", pageable).getTotalElements());
        assertEquals(1, preguntaService.filtrarPreguntasCompletoPaginado("_1LS", "X", "borrador,aprobada",
            "geo", "sub", "p", "r", "guion", "texto", pageable).getTotalElements());
        assertEquals(1, preguntaService.obtenerPaginadasDTO(pageable).getTotalElements());
    }

    @Test
    void eliminarPorId_okYUsada() {
        Pregunta p = preguntaBase();
        when(preguntaRepository.findById(1L)).thenReturn(Optional.of(p));
        preguntaService.eliminarPorId(1L);
        verify(preguntaRepository).deleteById(1L);

        Pregunta usada = preguntaBase();
        usada.setEstado(EstadoPregunta.usada);
        when(preguntaRepository.findById(2L)).thenReturn(Optional.of(usada));
        assertThrows(IllegalArgumentException.class, () -> preguntaService.eliminarPorId(2L));
    }

    @Test
    void mapPreguntaToDTO_yEstadisticasYaCubiertasIndirectamente() {
        PreguntaDTO dto = preguntaService.mapPreguntaToDTO(preguntaBase());
        assertNotNull(dto.getId());
        assertEquals("Capital de Francia", dto.getPregunta());
    }
}
