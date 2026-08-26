package com.lsnls.service;

import com.lsnls.dto.CrearCuestionarioDTO;
import com.lsnls.entity.Cuestionario;
import com.lsnls.entity.Cuestionario.EstadoCuestionario;
import com.lsnls.entity.Cuestionario.NivelCuestionario;
import com.lsnls.entity.Pregunta;
import com.lsnls.entity.PreguntaCuestionario;
import com.lsnls.entity.Usuario;
import com.lsnls.repository.CuestionarioRepository;
import com.lsnls.repository.PreguntaCuestionarioRepository;
import com.lsnls.repository.PreguntaRepository;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
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
class CuestionarioServiceTest {

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
        when(nativeQuery.getSingleResult()).thenReturn(0L);
        when(nativeQuery.getResultList()).thenReturn(Collections.emptyList());
        when(nativeQuery.executeUpdate()).thenReturn(1);

        when(undoService.snapshotFila(anyString(), any())).thenReturn(Collections.singletonMap("id", 1L));
        when(undoService.snapshotFilas(anyString(), anyString(), any())).thenReturn(Collections.emptyList());
        when(cuestionarioRepository.save(any(Cuestionario.class))).thenAnswer(inv -> {
            Cuestionario c = inv.getArgument(0);
            if (c.getId() == null) {
                c.setId(1L);
            }
            return c;
        });
    }

    private Usuario usuarioMinimo() {
        Usuario u = new Usuario();
        u.setId(2L);
        u.setNombre("dir");
        u.setRol(Usuario.RolUsuario.ROLE_DIRECCION);
        return u;
    }

    private Cuestionario cuestionarioBase() {
        Cuestionario c = new Cuestionario();
        c.setId(1L);
        c.setEstado(EstadoCuestionario.borrador);
        c.setNivel(NivelCuestionario.NORMAL);
        c.setTematica("Historia");
        c.setCreacionUsuario(usuarioMinimo());
        c.setPreguntas(new HashSet<>());
        return c;
    }

    private Pregunta preguntaAprobada(Long id, Pregunta.NivelPregunta nivel) {
        Pregunta p = new Pregunta();
        p.setId(id);
        p.setNivel(nivel);
        p.setEstado(Pregunta.EstadoPregunta.aprobada);
        p.setEstadoDisponibilidad(Pregunta.EstadoDisponibilidad.disponible);
        p.setPregunta("P" + id);
        p.setRespuesta("R" + id);
        p.setTematica("T");
        p.setCreacionUsuario(usuarioMinimo());
        return p;
    }

    @Test
    void crear_fuerzaBorrador() {
        Cuestionario c = new Cuestionario();
        c.setNivel(NivelCuestionario._1LS);
        c.setEstado(EstadoCuestionario.aprobado);
        c.setCreacionUsuario(usuarioMinimo());

        Cuestionario saved = cuestionarioService.crear(c);

        assertEquals(EstadoCuestionario.borrador, saved.getEstado());
        assertNotNull(saved.getFechaCreacion());
    }

    @Test
    void obtenerTodos_yPorId() {
        when(cuestionarioRepository.findAllOrderByIdDesc()).thenReturn(Collections.singletonList(cuestionarioBase()));
        when(cuestionarioRepository.findById(1L)).thenReturn(Optional.of(cuestionarioBase()));
        when(cuestionarioRepository.findById(9L)).thenReturn(Optional.empty());

        assertEquals(1, cuestionarioService.obtenerTodos().size());
        assertTrue(cuestionarioService.obtenerPorId(1L).isPresent());
        assertFalse(cuestionarioService.obtenerPorId(9L).isPresent());
    }

    @Test
    void validarTransicionEstado_mismasONulasOk() {
        cuestionarioService.validarTransicionEstado(null, EstadoCuestionario.revisar, false);
        cuestionarioService.validarTransicionEstado(EstadoCuestionario.borrador, EstadoCuestionario.borrador, false);
    }

    @Test
    void validarTransicionEstado_adminPuedeCualquiera() {
        cuestionarioService.validarTransicionEstado(EstadoCuestionario.aprobado, EstadoCuestionario.borrador, true);
    }

    @Test
    void validarTransicionEstado_noAdminBorradorARevisarOk() {
        cuestionarioService.validarTransicionEstado(EstadoCuestionario.borrador, EstadoCuestionario.revisar, false);
    }

    @Test
    void validarTransicionEstado_noAdminProhibida() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> cuestionarioService.validarTransicionEstado(
                EstadoCuestionario.borrador, EstadoCuestionario.aprobado, false));
        assertTrue(ex.getMessage().contains("no permitida"));
    }

    @Test
    void cambiarEstado_noEncontrado() {
        when(typedQuery.getResultList()).thenReturn(Collections.emptyList());
        assertNull(cuestionarioService.cambiarEstado(1L, EstadoCuestionario.revisar, true));
    }

    @Test
    void cambiarEstado_adminARevisarSinCompletar() {
        Cuestionario c = cuestionarioBase();
        when(typedQuery.getResultList()).thenReturn(Collections.singletonList(c));

        Cuestionario result = cuestionarioService.cambiarEstado(1L, EstadoCuestionario.revisar, true);

        assertEquals(EstadoCuestionario.revisar, result.getEstado());
        verify(cuestionarioRepository).save(c);
    }

    @Test
    void cambiarEstado_noAdminRevisarExigeCompleto() {
        Cuestionario c = cuestionarioBase();
        when(typedQuery.getResultList()).thenReturn(Collections.singletonList(c));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> cuestionarioService.cambiarEstado(1L, EstadoCuestionario.revisar, false));
        assertTrue(ex.getMessage().contains("exactamente 4"));
    }

    @Test
    void cambiarEstado_noAdminAprobadoTambienExigeCompleto() {
        Cuestionario c = cuestionarioBase();
        c.setEstado(EstadoCuestionario.revisar);
        when(typedQuery.getResultList()).thenReturn(Collections.singletonList(c));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> cuestionarioService.cambiarEstado(1L, EstadoCuestionario.aprobado, false));
        assertTrue(ex.getMessage().contains("exactamente 4"));
    }

    @Test
    void agregarPregunta_cuestionarioNoExiste() {
        when(cuestionarioRepository.findById(1L)).thenReturn(Optional.empty());
        when(preguntaRepository.findById(5L)).thenReturn(Optional.of(preguntaAprobada(5L, Pregunta.NivelPregunta._1LS)));
        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> cuestionarioService.agregarPregunta(1L, 5L, 1));
        assertTrue(ex.getMessage().contains("Cuestionario no encontrado"));
    }

    @Test
    void agregarPregunta_preguntaNoAprobada() {
        Pregunta p = preguntaAprobada(5L, Pregunta.NivelPregunta._1LS);
        p.setEstado(Pregunta.EstadoPregunta.borrador);
        when(cuestionarioRepository.findById(1L)).thenReturn(Optional.of(cuestionarioBase()));
        when(preguntaRepository.findById(5L)).thenReturn(Optional.of(p));
        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> cuestionarioService.agregarPregunta(1L, 5L, 1));
        assertTrue(ex.getMessage().contains("aprobada"));
    }

    @Test
    void agregarPregunta_yaExiste() {
        when(cuestionarioRepository.findById(1L)).thenReturn(Optional.of(cuestionarioBase()));
        when(preguntaRepository.findById(5L)).thenReturn(Optional.of(preguntaAprobada(5L, Pregunta.NivelPregunta._1LS)));
        when(preguntaCuestionarioRepository.existsById(any())).thenReturn(true);
        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> cuestionarioService.agregarPregunta(1L, 5L, 1));
        assertTrue(ex.getMessage().contains("ya está agregada"));
    }

    @Test
    void agregarPregunta_okPromueveVerificada() {
        Pregunta p = preguntaAprobada(5L, Pregunta.NivelPregunta._1LS);
        p.setEstado(Pregunta.EstadoPregunta.verificada);
        when(cuestionarioRepository.findById(1L)).thenReturn(Optional.of(cuestionarioBase()));
        when(preguntaRepository.findById(5L)).thenReturn(Optional.of(p));
        when(preguntaCuestionarioRepository.existsById(any())).thenReturn(false);

        assertTrue(cuestionarioService.agregarPregunta(1L, 5L, null));
        verify(preguntaCuestionarioRepository).save(any(PreguntaCuestionario.class));
        verify(nativeQuery, org.mockito.Mockito.atLeastOnce()).executeUpdate();
    }

    @Test
    void quitarPregunta_sinRelacion() {
        when(cuestionarioRepository.findById(1L)).thenReturn(Optional.of(cuestionarioBase()));
        when(preguntaRepository.findById(5L)).thenReturn(Optional.of(preguntaAprobada(5L, Pregunta.NivelPregunta._1LS)));
        when(preguntaCuestionarioRepository.existsById(any())).thenReturn(false);
        assertFalse(cuestionarioService.quitarPregunta(1L, 5L));
    }

    @Test
    void quitarPregunta_ok() {
        Pregunta p = preguntaAprobada(5L, Pregunta.NivelPregunta._1LS);
        p.setEstadoDisponibilidad(Pregunta.EstadoDisponibilidad.usada);
        when(cuestionarioRepository.findById(1L)).thenReturn(Optional.of(cuestionarioBase()));
        when(preguntaRepository.findById(5L)).thenReturn(Optional.of(p));
        when(preguntaCuestionarioRepository.existsById(any())).thenReturn(true);
        when(nativeQuery.executeUpdate()).thenReturn(1);

        assertTrue(cuestionarioService.quitarPregunta(1L, 5L));
    }

    @Test
    void quitarPregunta_entidadesFaltantes() {
        when(cuestionarioRepository.findById(1L)).thenReturn(Optional.empty());
        when(preguntaRepository.findById(5L)).thenReturn(Optional.empty());
        assertFalse(cuestionarioService.quitarPregunta(1L, 5L));
    }

    @Test
    void eliminar_noExiste() {
        when(cuestionarioRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> cuestionarioService.eliminar(1L));
    }

    @Test
    void eliminar_adjudicado() {
        Cuestionario c = cuestionarioBase();
        c.setEstado(EstadoCuestionario.adjudicado);
        when(cuestionarioRepository.findById(1L)).thenReturn(Optional.of(c));
        assertThrows(IllegalArgumentException.class, () -> cuestionarioService.eliminar(1L));
    }

    @Test
    void eliminar_grabado() {
        Cuestionario c = cuestionarioBase();
        c.setEstado(EstadoCuestionario.grabado);
        when(cuestionarioRepository.findById(1L)).thenReturn(Optional.of(c));
        assertThrows(IllegalArgumentException.class, () -> cuestionarioService.eliminar(1L));
    }

    @Test
    void eliminar_conConcursantes() {
        when(cuestionarioRepository.findById(1L)).thenReturn(Optional.of(cuestionarioBase()));
        when(typedQuery.getSingleResult()).thenReturn(2L);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> cuestionarioService.eliminar(1L));
        assertTrue(ex.getMessage().contains("concursante"));
    }

    @Test
    void eliminar_conJornadas() {
        when(cuestionarioRepository.findById(1L)).thenReturn(Optional.of(cuestionarioBase()));
        when(typedQuery.getSingleResult()).thenReturn(0L, 1L);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> cuestionarioService.eliminar(1L));
        assertTrue(ex.getMessage().contains("jornada"));
    }

    @Test
    void eliminar_ok() {
        Cuestionario c = cuestionarioBase();
        Pregunta p = preguntaAprobada(7L, Pregunta.NivelPregunta._1LS);
        PreguntaCuestionario pc = new PreguntaCuestionario();
        pc.setPregunta(p);
        c.setPreguntas(new HashSet<>(Collections.singletonList(pc)));
        when(cuestionarioRepository.findById(1L)).thenReturn(Optional.of(c));

        cuestionarioService.eliminar(1L);

        verify(cuestionarioRepository).deleteById(1L);
        verify(undoService).registrar(eq("eliminar_cuestionario"), anyString(), any());
    }

    @Test
    void crearDesdeDTO_okListaVacia() {
        CrearCuestionarioDTO dto = new CrearCuestionarioDTO();
        dto.setPreguntasNormales(new ArrayList<>());
        dto.setTematica("Arte");
        when(cuestionarioRepository.findById(1L)).thenReturn(Optional.of(cuestionarioBase()));

        Cuestionario result = cuestionarioService.crearDesdeDTO(dto, usuarioMinimo());

        assertNotNull(result);
        verify(cuestionarioRepository).save(any(Cuestionario.class));
    }

    @Test
    void crearDesdeDTO_reservaYRelacionaPregunta() {
        CrearCuestionarioDTO dto = new CrearCuestionarioDTO();
        dto.setPreguntasNormales(Collections.singletonList(5L));
        Pregunta p = preguntaAprobada(5L, Pregunta.NivelPregunta._1LS);
        when(preguntaRepository.findAllById(any())).thenReturn(Collections.singletonList(p));
        when(preguntaRepository.findById(5L)).thenReturn(Optional.of(p));
        when(nativeQuery.executeUpdate()).thenReturn(1);
        when(cuestionarioRepository.findById(1L)).thenReturn(Optional.of(cuestionarioBase()));

        Cuestionario result = cuestionarioService.crearDesdeDTO(dto, usuarioMinimo());

        assertNotNull(result);
        verify(preguntaCuestionarioRepository).save(any(PreguntaCuestionario.class));
    }

    @Test
    void estaAsignadoAJornada_falseYtrue() {
        when(typedQuery.getSingleResult()).thenReturn(0L);
        assertFalse(cuestionarioService.estaAsignadoAJornada(1L));
        when(typedQuery.getSingleResult()).thenReturn(2L);
        assertTrue(cuestionarioService.estaAsignadoAJornada(1L));
    }

    @Test
    void obtenerTematicasDisponibles() {
        when(tematicaService.obtenerNombresTematicas()).thenReturn(Arrays.asList("Cine", "Deporte"));
        List<String> result = cuestionarioService.obtenerTematicasDisponibles();
        assertEquals(2, result.size());
        assertEquals("Cine", result.get(0));
    }
}
