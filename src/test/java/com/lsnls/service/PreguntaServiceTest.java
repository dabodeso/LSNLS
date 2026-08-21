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
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PreguntaServiceTest {

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
        when(entityManager.createQuery(anyString(), any(Class.class))).thenReturn((TypedQuery) typedQuery);
        when(entityManager.createQuery(anyString())).thenReturn(typedQuery);
        when(typedQuery.setParameter(anyString(), nullable(Object.class))).thenReturn(typedQuery);
        when(typedQuery.setParameter(anyInt(), nullable(Object.class))).thenReturn(typedQuery);
        when(typedQuery.getSingleResult()).thenReturn(0L);
        when(typedQuery.getResultList()).thenReturn(Collections.emptyList());

        when(entityManager.createNativeQuery(anyString())).thenReturn(nativeQuery);
        when(nativeQuery.setParameter(anyString(), nullable(Object.class))).thenReturn(nativeQuery);
        when(nativeQuery.setParameter(anyInt(), nullable(Object.class))).thenReturn(nativeQuery);
        when(nativeQuery.getSingleResult()).thenReturn(0L);
        when(nativeQuery.getResultList()).thenReturn(Collections.emptyList());
        when(nativeQuery.executeUpdate()).thenReturn(1);

        when(dataTransformationService.normalizarPregunta(nullable(String.class)))
            .thenAnswer(inv -> inv.getArgument(0));
        when(dataTransformationService.normalizarRespuesta(nullable(String.class)))
            .thenAnswer(inv -> inv.getArgument(0));
        when(dataTransformationService.normalizarTematica(nullable(String.class)))
            .thenAnswer(inv -> inv.getArgument(0));
        when(dataTransformationService.validarPreguntaCompleta(
            nullable(String.class), nullable(String.class), nullable(String.class)))
            .thenReturn(new DataTransformationService.ValidationResult());

        when(undoService.snapshotFila(anyString(), any())).thenReturn(Collections.singletonMap("id", 1L));
        when(preguntaRepository.save(any(Pregunta.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private Usuario usuarioMinimo() {
        Usuario u = new Usuario();
        u.setId(3L);
        u.setNombre("guion");
        u.setRol(Usuario.RolUsuario.ROLE_GUION);
        return u;
    }

    private Pregunta preguntaBase() {
        Pregunta p = new Pregunta();
        p.setId(1L);
        p.setVersion(0L);
        p.setPregunta("Capital de Francia");
        p.setRespuesta("PARIS");
        p.setTematica("GEOGRAFIA");
        p.setFuentes("atlas");
        p.setNivel(NivelPregunta._1LS);
        p.setEstado(EstadoPregunta.borrador);
        p.setEstadoDisponibilidad(EstadoDisponibilidad.disponible);
        p.setCreacionUsuario(usuarioMinimo());
        p.setFechaCreacion(LocalDateTime.of(2026, 1, 1, 10, 0));
        p.setFactor(FactorPregunta.X);
        return p;
    }

    @Test
    void crear_okConDefaults() {
        Pregunta p = new Pregunta();
        p.setPregunta("texto");
        p.setRespuesta("resp");
        p.setTematica("tema");
        p.setNivel(NivelPregunta._2NLS);

        Pregunta saved = preguntaService.crear(p);

        assertEquals(EstadoPregunta.borrador, saved.getEstado());
        assertEquals(EstadoDisponibilidad.disponible, saved.getEstadoDisponibilidad());
        assertNotNull(saved.getFechaCreacion());
        verify(preguntaRepository).save(p);
    }

    @Test
    void crear_datosInvalidos() {
        DataTransformationService.ValidationResult invalid = new DataTransformationService.ValidationResult();
        invalid.addError("pregunta", "vacia");
        when(dataTransformationService.validarPreguntaCompleta(any(), any(), any())).thenReturn(invalid);

        Pregunta p = preguntaBase();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> preguntaService.crear(p));
        assertTrue(ex.getMessage().contains("Datos no válidos"));
    }

    @Test
    void validarRequisitosParaVerificar_faltanCampos() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> preguntaService.validarRequisitosParaVerificar(" ", "", null, "  "));
        assertTrue(ex.getMessage().contains("temática"));
        assertTrue(ex.getMessage().contains("pregunta"));
        assertTrue(ex.getMessage().contains("respuesta"));
        assertTrue(ex.getMessage().contains("fuente"));
    }

    @Test
    void validarRequisitosParaVerificar_ok() {
        preguntaService.validarRequisitosParaVerificar("tema", "preg", "resp", "wiki");
    }

    @Test
    void obtenerTodas_yPorId() {
        when(preguntaRepository.findAll()).thenReturn(Collections.singletonList(preguntaBase()));
        when(preguntaRepository.findById(1L)).thenReturn(Optional.of(preguntaBase()));
        when(preguntaRepository.findById(9L)).thenReturn(Optional.empty());

        assertEquals(1, preguntaService.obtenerTodas().size());
        assertTrue(preguntaService.obtenerPorId(1L).isPresent());
        assertFalse(preguntaService.obtenerPorId(9L).isPresent());
    }

    @Test
    void obtenerPorEstadoNivelYDisponibles() {
        Pregunta p = preguntaBase();
        when(preguntaRepository.findByEstado(EstadoPregunta.aprobada)).thenReturn(Collections.singletonList(p));
        when(preguntaRepository.findByNivel(NivelPregunta._1LS)).thenReturn(Collections.singletonList(p));
        when(preguntaRepository.findByEstadoAndEstadoDisponibilidad(
            EstadoPregunta.aprobada, EstadoDisponibilidad.disponible)).thenReturn(Collections.singletonList(p));

        assertEquals(1, preguntaService.obtenerPorEstado(EstadoPregunta.aprobada).size());
        assertEquals(1, preguntaService.obtenerPorNivel(NivelPregunta._1LS).size());
        assertEquals(1, preguntaService.obtenerDisponibles().size());
    }

    @Test
    void actualizar_noExiste() {
        when(preguntaRepository.existsById(1L)).thenReturn(false);
        assertNull(preguntaService.actualizar(1L, preguntaBase()));
    }

    @Test
    void actualizar_okMantieneCamposNulos() {
        Pregunta existente = preguntaBase();
        existente.setNotasVerificacion("nv");
        existente.setVerificacion("guion");
        when(preguntaRepository.existsById(1L)).thenReturn(true);
        when(preguntaRepository.findById(1L)).thenReturn(Optional.of(existente));

        Pregunta incoming = new Pregunta();
        incoming.setEstado(EstadoPregunta.borrador);

        Pregunta result = preguntaService.actualizar(1L, incoming);

        assertEquals("Capital de Francia", result.getPregunta());
        assertEquals("GEOGRAFIA", result.getTematica());
        assertEquals("nv", result.getNotasVerificacion());
        assertEquals("guion", result.getVerificacion());
    }

    @Test
    void cambiarEstado_noEncontrada() {
        when(preguntaRepository.findById(1L)).thenReturn(Optional.empty());
        assertNull(preguntaService.cambiarEstado(1L, EstadoPregunta.aprobada));
    }

    @Test
    void cambiarEstado_aVerificadaYAprobada() {
        Pregunta p = preguntaBase();
        when(preguntaRepository.findById(1L)).thenReturn(Optional.of(p));

        Pregunta verificada = preguntaService.cambiarEstado(1L, EstadoPregunta.verificada, usuarioMinimo());
        assertEquals(EstadoPregunta.verificada, verificada.getEstado());
        assertNotNull(verificada.getFechaVerificacion());
        assertEquals(3L, verificada.getVerificacionUsuario().getId());

        Pregunta aprobada = preguntaService.cambiarEstado(1L, EstadoPregunta.aprobada);
        assertEquals(EstadoDisponibilidad.disponible, aprobada.getEstadoDisponibilidad());
    }

    @Test
    void marcarComoUsada_desdeAprobada() {
        Pregunta p = preguntaBase();
        p.setEstado(EstadoPregunta.aprobada);
        when(preguntaRepository.findById(1L)).thenReturn(Optional.of(p));

        preguntaService.marcarComoUsada(1L);

        assertEquals(EstadoDisponibilidad.usada, p.getEstadoDisponibilidad());
        assertEquals(EstadoPregunta.usada, p.getEstado());
        verify(preguntaRepository).save(p);
    }

    @Test
    void liberarPregunta_desdeUsada() {
        Pregunta p = preguntaBase();
        p.setEstado(EstadoPregunta.usada);
        p.setEstadoDisponibilidad(EstadoDisponibilidad.usada);
        when(preguntaRepository.findById(1L)).thenReturn(Optional.of(p));

        preguntaService.liberarPregunta(1L);

        assertEquals(EstadoDisponibilidad.liberada, p.getEstadoDisponibilidad());
        assertEquals(EstadoPregunta.aprobada, p.getEstado());
    }

    @Test
    void eliminar_noExiste() {
        when(preguntaRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> preguntaService.eliminar(1L));
    }

    @Test
    void eliminar_conCuestionarios() {
        when(preguntaRepository.findById(1L)).thenReturn(Optional.of(preguntaBase()));
        when(typedQuery.getSingleResult()).thenReturn(2L);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> preguntaService.eliminar(1L));
        assertTrue(ex.getMessage().contains("cuestionario"));
    }

    @Test
    void eliminar_conCombos() {
        when(preguntaRepository.findById(1L)).thenReturn(Optional.of(preguntaBase()));
        when(typedQuery.getSingleResult()).thenReturn(0L, 4L);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> preguntaService.eliminar(1L));
        assertTrue(ex.getMessage().contains("combo"));
    }

    @Test
    void eliminar_ok() {
        when(preguntaRepository.findById(1L)).thenReturn(Optional.of(preguntaBase()));
        preguntaService.eliminar(1L);
        verify(preguntaRepository).deleteById(1L);
        verify(undoService).snapshotFila("preguntas", 1L);
        verify(undoService).registrar(eq("eliminar_pregunta"), anyString(), any());
    }

    @Test
    void mapPreguntaToDTO_conYsinUsuario() {
        Pregunta p = preguntaBase();
        PreguntaDTO dto = preguntaService.mapPreguntaToDTO(p);
        assertEquals(1L, dto.getId());
        assertEquals("Capital de Francia", dto.getPregunta());
        assertEquals(NivelPregunta._1LS, dto.getNivel());
        assertEquals("borrador", dto.getEstado());
        assertEquals(3L, dto.getCreacionUsuarioId());
        assertEquals("guion", dto.getCreacionUsuarioNombre());

        p.setCreacionUsuario(null);
        p.setFechaCreacion(null);
        p.setEstado(null);
        PreguntaDTO dto2 = preguntaService.mapPreguntaToDTO(p);
        assertNull(dto2.getCreacionUsuarioId());
        assertNull(dto2.getFechaCreacion());
        assertNull(dto2.getEstado());
    }

    @Test
    void obtenerTodasDTO() {
        when(preguntaRepository.findAll()).thenReturn(Collections.singletonList(preguntaBase()));
        List<PreguntaDTO> dtos = preguntaService.obtenerTodasDTO();
        assertEquals(1, dtos.size());
        assertEquals(1L, dtos.get(0).getId());
    }

    @Test
    void obtenerEstadisticasNiveles() {
        Pregunta a = preguntaBase();
        Pregunta b = preguntaBase();
        b.setId(2L);
        b.setNivel(NivelPregunta._5LS);
        b.setEstado(EstadoPregunta.aprobada);
        b.setEstadoDisponibilidad(EstadoDisponibilidad.usada);
        when(preguntaRepository.findAll()).thenReturn(Arrays.asList(a, b));

        Map<String, Object> stats = preguntaService.obtenerEstadisticasNiveles();

        assertEquals(2, stats.get("totalPreguntas"));
        assertNotNull(stats.get("porNivel"));
        assertNotNull(stats.get("porEstado"));
        assertNotNull(stats.get("porDisponibilidad"));
    }

    @Test
    void filtrarPreguntasCompleto_mapeaResultados() {
        when(preguntaRepository.findAll(any(Specification.class), any(Sort.class)))
            .thenReturn(Collections.singletonList(preguntaBase()));

        List<PreguntaDTO> result = preguntaService.filtrarPreguntasCompleto(
            "_1LS", "X", "borrador", "GEO", null, null, null, null, null);

        assertEquals(1, result.size());
        assertEquals("GEOGRAFIA", result.get(0).getTematica());
    }

    @Test
    void filtrarPreguntasCompleto_estadosCsvYNivelSinGuion() {
        when(preguntaRepository.findAll(any(Specification.class), any(Sort.class)))
            .thenReturn(Collections.emptyList());

        List<PreguntaDTO> result = preguntaService.filtrarPreguntasCompleto(
            "1LS", "NOPE", "borrador,aprobada,invalido", "tema", "sub", "preg", "resp", "autor", "texto");

        assertTrue(result.isEmpty());
    }
}
