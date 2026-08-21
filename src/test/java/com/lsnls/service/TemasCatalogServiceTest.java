package com.lsnls.service;

import com.lsnls.entity.SubtemaPregunta;
import com.lsnls.entity.TematicaCombo;
import com.lsnls.entity.TematicaPregunta;
import com.lsnls.entity.Usuario;
import com.lsnls.repository.SubtemaPreguntaRepository;
import com.lsnls.repository.TematicaComboRepository;
import com.lsnls.repository.TematicaPreguntaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TemasCatalogServiceTest {

    @Mock
    private TematicaPreguntaRepository tematicaPreguntaRepository;
    @Mock
    private TematicaComboRepository tematicaComboRepository;
    @Mock
    private SubtemaPreguntaRepository subtemaPreguntaRepository;
    @Mock
    private AuthorizationService authorizationService;

    @Mock
    private UndoService undoService;

    @InjectMocks
    private TemasCatalogService service;

    @Test
    void obtenerListasOrdenadas() {
        TematicaPregunta tp = new TematicaPregunta();
        tp.setNombre("ZETA");
        TematicaPregunta tp2 = new TematicaPregunta();
        tp2.setNombre("ALFA");
        when(tematicaPreguntaRepository.findAll()).thenReturn(Arrays.asList(tp, tp2));

        TematicaCombo tc = new TematicaCombo();
        tc.setNombre("COMBO");
        when(tematicaComboRepository.findAll()).thenReturn(Collections.singletonList(tc));

        SubtemaPregunta sp = new SubtemaPregunta();
        sp.setNombre("SUB");
        when(subtemaPreguntaRepository.findAll()).thenReturn(Collections.singletonList(sp));

        List<String> tematicas = service.obtenerTematicasPreguntas();
        assertEquals(Arrays.asList("ALFA", "ZETA"), tematicas);
        assertEquals(Collections.singletonList("COMBO"), service.obtenerTematicasCombos());
        assertEquals(Collections.singletonList("SUB"), service.obtenerSubtemasPreguntas());
    }

    @Test
    void añadirTematicaPregunta_existente() {
        when(tematicaPreguntaRepository.existsByNombre("HISTORIA")).thenReturn(true);
        assertEquals("HISTORIA", service.añadirTematicaPregunta("  historia  "));
        verify(tematicaPreguntaRepository, never()).save(any());
    }

    @Test
    void añadirTematicaPregunta_nuevaConUsuario() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        when(tematicaPreguntaRepository.existsByNombre("ARTE")).thenReturn(false);
        when(authorizationService.getCurrentUser()).thenReturn(Optional.of(usuario));
        when(tematicaPreguntaRepository.save(any(TematicaPregunta.class))).thenAnswer(inv -> {
            TematicaPregunta t = inv.getArgument(0);
            t.setId(8L);
            return t;
        });

        assertEquals("ARTE", service.añadirTematicaPregunta("arte"));
        ArgumentCaptor<TematicaPregunta> captor = ArgumentCaptor.forClass(TematicaPregunta.class);
        verify(tematicaPreguntaRepository).save(captor.capture());
        assertEquals("ARTE", captor.getValue().getNombre());
        assertEquals(usuario, captor.getValue().getCreacionUsuario());
        verify(undoService).registrar(eq("añadir_tematica_pregunta"), anyString(), any());
    }

    @Test
    void añadirSubtemaPregunta_existenteYNuevaSinUsuario() {
        when(subtemaPreguntaRepository.existsByNombre("SUB")).thenReturn(true);
        assertEquals("SUB", service.añadirSubtemaPregunta("sub"));

        when(subtemaPreguntaRepository.existsByNombre("NUEVO")).thenReturn(false);
        when(authorizationService.getCurrentUser()).thenReturn(Optional.empty());
        when(subtemaPreguntaRepository.save(any(SubtemaPregunta.class))).thenAnswer(inv -> inv.getArgument(0));
        assertEquals("NUEVO", service.añadirSubtemaPregunta("nuevo"));
        ArgumentCaptor<SubtemaPregunta> captor = ArgumentCaptor.forClass(SubtemaPregunta.class);
        verify(subtemaPreguntaRepository).save(captor.capture());
        assertNull(captor.getValue().getCreacionUsuario());
    }

    @Test
    void añadirConNull() {
        when(tematicaPreguntaRepository.existsByNombre(null)).thenReturn(true);
        assertNull(service.añadirTematicaPregunta(null));
    }

    @Test
    void eliminarTematicaYSubtema() {
        TematicaPregunta tp = new TematicaPregunta();
        when(tematicaPreguntaRepository.findByNombre("HISTORIA")).thenReturn(Optional.of(tp));
        service.eliminarTematicaPregunta("historia");
        verify(tematicaPreguntaRepository).delete(tp);

        when(tematicaPreguntaRepository.findByNombre("NO")).thenReturn(Optional.empty());
        service.eliminarTematicaPregunta("no");
        verify(tematicaPreguntaRepository).delete(tp);

        SubtemaPregunta sp = new SubtemaPregunta();
        when(subtemaPreguntaRepository.findByNombre("SUB")).thenReturn(Optional.of(sp));
        service.eliminarSubtemaPregunta("sub");
        verify(subtemaPreguntaRepository).delete(sp);

        when(subtemaPreguntaRepository.findByNombre("X")).thenReturn(Optional.empty());
        service.eliminarSubtemaPregunta("x");
    }
}
