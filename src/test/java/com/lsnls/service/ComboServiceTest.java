package com.lsnls.service;

import com.lsnls.dto.CrearComboDTO;
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

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
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
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ComboServiceTest {

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
        when(nativeQuery.getSingleResult()).thenReturn(0L);
        when(nativeQuery.getResultList()).thenReturn(Collections.emptyList());
        when(nativeQuery.executeUpdate()).thenReturn(1);

        when(undoService.snapshotFila(anyString(), any())).thenReturn(Collections.singletonMap("id", 1L));
        when(undoService.snapshotFilas(anyString(), anyString(), any())).thenReturn(Collections.emptyList());
        when(comboRepository.save(any(Combo.class))).thenAnswer(inv -> {
            Combo c = inv.getArgument(0);
            if (c.getId() == null) {
                c.setId(1L);
            }
            return c;
        });
    }

    private Usuario usuarioMinimo() {
        Usuario u = new Usuario();
        u.setId(4L);
        u.setNombre("guion");
        u.setRol(Usuario.RolUsuario.ROLE_GUION);
        return u;
    }

    private Combo comboBase() {
        Combo c = new Combo();
        c.setId(1L);
        c.setEstado(EstadoCombo.borrador);
        c.setNivel(NivelCombo.NORMAL);
        c.setTipo(Combo.TipoCombo.P);
        c.setTematica("Cine");
        c.setCreacionUsuario(usuarioMinimo());
        c.setPreguntas(new HashSet<>());
        return c;
    }

    private Pregunta preguntaNivel5(Long id) {
        Pregunta p = new Pregunta();
        p.setId(id);
        p.setNivel(Pregunta.NivelPregunta._5LS);
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
        Combo c = new Combo();
        c.setNivel(NivelCombo._5LS);
        c.setEstado(EstadoCombo.aprobado);
        c.setCreacionUsuario(usuarioMinimo());

        Combo saved = comboService.crear(c);

        assertEquals(EstadoCombo.borrador, saved.getEstado());
        assertNotNull(saved.getFechaCreacion());
    }

    @Test
    void obtenerTodos_yPorId() {
        when(comboRepository.findAll()).thenReturn(Collections.singletonList(comboBase()));
        when(comboRepository.findById(1L)).thenReturn(Optional.of(comboBase()));
        when(comboRepository.findById(9L)).thenReturn(Optional.empty());

        assertEquals(1, comboService.obtenerTodos().size());
        assertTrue(comboService.obtenerPorId(1L).isPresent());
        assertFalse(comboService.obtenerPorId(9L).isPresent());
    }

    @Test
    void validarTransicionEstado_mismasONulasOk() {
        comboService.validarTransicionEstado(null, EstadoCombo.revisar, false);
        comboService.validarTransicionEstado(EstadoCombo.borrador, EstadoCombo.borrador, false);
    }

    @Test
    void validarTransicionEstado_adminPuedeCualquiera() {
        comboService.validarTransicionEstado(EstadoCombo.aprobado, EstadoCombo.borrador, true);
    }

    @Test
    void validarTransicionEstado_noAdminOkYProhibida() {
        comboService.validarTransicionEstado(EstadoCombo.borrador, EstadoCombo.revisar, false);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> comboService.validarTransicionEstado(EstadoCombo.borrador, EstadoCombo.aprobado, false));
        assertTrue(ex.getMessage().contains("no permitida"));
    }

    @Test
    void cambiarEstado_noEncontrado() {
        when(typedQuery.getResultList()).thenReturn(Collections.emptyList());
        assertNull(comboService.cambiarEstado(1L, EstadoCombo.revisar));
    }

    @Test
    void cambiarEstado_adminARevisarSinCompletar() {
        Combo c = comboBase();
        when(typedQuery.getResultList()).thenReturn(Collections.singletonList(c));

        Combo result = comboService.cambiarEstado(1L, EstadoCombo.revisar, true);

        assertEquals(EstadoCombo.revisar, result.getEstado());
        verify(comboRepository).save(c);
    }

    @Test
    void cambiarEstado_noAdminExigeCompleto() {
        Combo c = comboBase();
        when(typedQuery.getResultList()).thenReturn(Collections.singletonList(c));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> comboService.cambiarEstado(1L, EstadoCombo.revisar, false));
        assertTrue(ex.getMessage().contains("exactamente 3"));
    }

    @Test
    void agregarPregunta_entidadesFaltantes() {
        when(comboRepository.findById(1L)).thenReturn(Optional.empty());
        when(preguntaRepository.findById(5L)).thenReturn(Optional.empty());
        assertFalse(comboService.agregarPregunta(1L, 5L, 2, 1));
    }

    @Test
    void agregarPregunta_noAprobada() {
        Pregunta p = preguntaNivel5(5L);
        p.setEstado(Pregunta.EstadoPregunta.borrador);
        when(comboRepository.findById(1L)).thenReturn(Optional.of(comboBase()));
        when(preguntaRepository.findById(5L)).thenReturn(Optional.of(p));
        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> comboService.agregarPregunta(1L, 5L, 2, 1));
        assertTrue(ex.getMessage().contains("aprobada"));
    }

    @Test
    void agregarPregunta_nivelIncorrecto() {
        Pregunta p = preguntaNivel5(5L);
        p.setNivel(Pregunta.NivelPregunta._1LS);
        when(comboRepository.findById(1L)).thenReturn(Optional.of(comboBase()));
        when(preguntaRepository.findById(5L)).thenReturn(Optional.of(p));
        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> comboService.agregarPregunta(1L, 5L, 2, 1));
        assertTrue(ex.getMessage().contains("nivel 5"));
    }

    @Test
    void agregarPregunta_yaExiste() {
        when(comboRepository.findById(1L)).thenReturn(Optional.of(comboBase()));
        when(preguntaRepository.findById(5L)).thenReturn(Optional.of(preguntaNivel5(5L)));
        when(preguntaComboRepository.existsById(any())).thenReturn(true);
        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> comboService.agregarPregunta(1L, 5L, 2, 1));
        assertTrue(ex.getMessage().contains("ya est"));
    }

    @Test
    void agregarPregunta_ok() {
        when(comboRepository.findById(1L)).thenReturn(Optional.of(comboBase()));
        when(preguntaRepository.findById(5L)).thenReturn(Optional.of(preguntaNivel5(5L)));
        when(preguntaComboRepository.existsById(any())).thenReturn(false);

        assertTrue(comboService.agregarPregunta(1L, 5L, 2, 1));
        verify(preguntaComboRepository).save(any(PreguntaCombo.class));
        verify(nativeQuery, atLeastOnce()).executeUpdate();
    }

    @Test
    void quitarPregunta_entidadesFaltantes() {
        when(comboRepository.findById(1L)).thenReturn(Optional.empty());
        assertFalse(comboService.quitarPregunta(1L, 5L));
    }

    @Test
    void quitarPregunta_okYLibera() {
        Pregunta p = preguntaNivel5(5L);
        p.setEstadoDisponibilidad(Pregunta.EstadoDisponibilidad.usada);
        when(comboRepository.findById(1L)).thenReturn(Optional.of(comboBase()));
        when(preguntaRepository.findById(5L)).thenReturn(Optional.of(p));
        when(nativeQuery.executeUpdate()).thenReturn(1);
        when(typedQuery.getSingleResult()).thenReturn(0L);

        assertTrue(comboService.quitarPregunta(1L, 5L));
    }

    @Test
    void eliminar_noExiste() {
        when(comboRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> comboService.eliminar(1L));
    }

    @Test
    void eliminar_adjudicado() {
        Combo c = comboBase();
        c.setEstado(EstadoCombo.adjudicado);
        when(comboRepository.findById(1L)).thenReturn(Optional.of(c));
        assertThrows(IllegalArgumentException.class, () -> comboService.eliminar(1L));
    }

    @Test
    void eliminar_grabado() {
        Combo c = comboBase();
        c.setEstado(EstadoCombo.grabado);
        when(comboRepository.findById(1L)).thenReturn(Optional.of(c));
        assertThrows(IllegalArgumentException.class, () -> comboService.eliminar(1L));
    }

    @Test
    void eliminar_conConcursantes() {
        when(comboRepository.findById(1L)).thenReturn(Optional.of(comboBase()));
        when(typedQuery.getSingleResult()).thenReturn(1L);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> comboService.eliminar(1L));
        assertTrue(ex.getMessage().contains("concursante"));
    }

    @Test
    void eliminar_conJornadas() {
        when(comboRepository.findById(1L)).thenReturn(Optional.of(comboBase()));
        when(typedQuery.getSingleResult()).thenReturn(0L, 2L);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> comboService.eliminar(1L));
        assertTrue(ex.getMessage().contains("jornada"));
    }

    @Test
    void eliminar_ok() {
        Combo c = comboBase();
        Pregunta p = preguntaNivel5(8L);
        PreguntaCombo pc = new PreguntaCombo();
        pc.setPregunta(p);
        c.setPreguntas(new HashSet<>(Collections.singletonList(pc)));
        when(comboRepository.findById(1L)).thenReturn(Optional.of(c));

        comboService.eliminar(1L);

        verify(comboRepository).deleteById(1L);
        verify(undoService).registrar(eq("eliminar_combo"), anyString(), any());
    }

    @Test
    void estaAsignadoAJornada_falseYtrue() {
        when(typedQuery.getSingleResult()).thenReturn(0L);
        assertFalse(comboService.estaAsignadoAJornada(1L));
        when(typedQuery.getSingleResult()).thenReturn(3L);
        assertTrue(comboService.estaAsignadoAJornada(1L));
    }

    @Test
    void contarPreguntasCombo_noExisteOVacio() {
        when(comboRepository.findById(1L)).thenReturn(Optional.empty());
        assertEquals(0, comboService.contarPreguntasCombo(1L));

        Combo c = comboBase();
        c.setPreguntas(null);
        when(comboRepository.findById(2L)).thenReturn(Optional.of(c));
        assertEquals(0, comboService.contarPreguntasCombo(2L));
    }

    @Test
    void contarPreguntasCombo_conPreguntas() {
        Combo c = comboBase();
        PreguntaCombo pc = new PreguntaCombo();
        c.setPreguntas(new HashSet<>(Collections.singletonList(pc)));
        when(comboRepository.findById(1L)).thenReturn(Optional.of(c));
        assertEquals(1, comboService.contarPreguntasCombo(1L));
    }

    @Test
    void crearComboDesdeDTO_ok() {
        CrearComboDTO dto = new CrearComboDTO();
        dto.setTipo("P");
        dto.setTematica("Cine");
        dto.setEstado("borrador");
        CrearComboDTO.PreguntaMultiplicadoraDTO pm = new CrearComboDTO.PreguntaMultiplicadoraDTO();
        pm.setId(5L);
        pm.setFactor("X2");
        dto.setPreguntasMultiplicadoras(Collections.singletonList(pm));

        Pregunta p = preguntaNivel5(5L);
        when(preguntaRepository.findAllById(any())).thenReturn(Collections.singletonList(p));
        when(preguntaRepository.findById(5L)).thenReturn(Optional.of(p));
        when(nativeQuery.executeUpdate()).thenReturn(1);
        when(comboRepository.findById(1L)).thenReturn(Optional.of(comboBase()));

        Combo result = comboService.crearComboDesdeDTO(dto, usuarioMinimo());

        assertNotNull(result);
        verify(preguntaComboRepository).save(any(PreguntaCombo.class));
    }

    @Test
    void crearComboDesdeDTO_preguntaNoEncontrada() {
        CrearComboDTO dto = new CrearComboDTO();
        dto.setTipo("A");
        CrearComboDTO.PreguntaMultiplicadoraDTO pm = new CrearComboDTO.PreguntaMultiplicadoraDTO();
        pm.setId(99L);
        pm.setFactor("X");
        dto.setPreguntasMultiplicadoras(Collections.singletonList(pm));
        when(preguntaRepository.findAllById(any())).thenReturn(Collections.emptyList());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> comboService.crearComboDesdeDTO(dto, usuarioMinimo()));
        assertTrue(ex.getMessage().contains("no fueron encontradas") || ex.getMessage().contains("faltantes"));
    }
}
