package com.lsnls.service;

import com.lsnls.entity.Tematica;
import com.lsnls.entity.Usuario;
import com.lsnls.repository.ComboRepository;
import com.lsnls.repository.CuestionarioRepository;
import com.lsnls.repository.TematicaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TematicaServiceTest {

    @Mock
    private TematicaRepository tematicaRepository;
    @Mock
    private CuestionarioRepository cuestionarioRepository;
    @Mock
    private ComboRepository comboRepository;

    @Mock
    private UndoService undoService;

    @InjectMocks
    private TematicaService service;

    private Usuario usuario;
    private Tematica tematica;

    @BeforeEach
    void setUp() {
        usuario = new Usuario();
        usuario.setId(1L);
        usuario.setNombre("admin");
        usuario.setRol(Usuario.RolUsuario.ROLE_ADMIN);
        tematica = new Tematica("HISTORIA", usuario);
        tematica.setId(10L);
    }

    @Test
    void obtenerTodasYBuscar() {
        when(tematicaRepository.findAllByOrderByNombreAsc()).thenReturn(Arrays.asList(tematica));
        when(tematicaRepository.findByNombreIgnoreCase("historia")).thenReturn(Optional.of(tematica));
        when(tematicaRepository.findByNombreContainingIgnoreCase("HIS")).thenReturn(Arrays.asList(tematica));
        when(tematicaRepository.findById(10L)).thenReturn(Optional.of(tematica));

        assertEquals(1, service.obtenerTodas().size());
        assertTrue(service.buscarPorNombre("historia").isPresent());
        assertEquals(1, service.buscarPorTexto("HIS").size());
        assertTrue(service.obtenerPorId(10L).isPresent());
    }

    @Test
    void crearTematica_yaExiste() {
        when(tematicaRepository.existsByNombreIgnoreCase("HISTORIA")).thenReturn(true);
        when(tematicaRepository.findByNombreIgnoreCase("HISTORIA")).thenReturn(Optional.of(tematica));
        Tematica result = service.crearTematica("HISTORIA", usuario);
        assertEquals(tematica, result);
        verify(tematicaRepository, never()).save(any());
    }

    @Test
    void crearTematica_nueva() {
        when(tematicaRepository.existsByNombreIgnoreCase("ARTE")).thenReturn(false);
        when(tematicaRepository.save(any(Tematica.class))).thenAnswer(inv -> inv.getArgument(0));
        Tematica result = service.crearTematica("ARTE", usuario);
        assertEquals("ARTE", result.getNombre());
    }

    @Test
    void añadirTematica_alias() {
        when(tematicaRepository.existsByNombreIgnoreCase("ARTE")).thenReturn(false);
        when(tematicaRepository.save(any(Tematica.class))).thenAnswer(inv -> inv.getArgument(0));
        assertEquals("ARTE", service.añadirTematica("ARTE", usuario).getNombre());
    }

    @Test
    void actualizarTematica_ok() {
        when(tematicaRepository.findById(10L)).thenReturn(Optional.of(tematica));
        when(tematicaRepository.existsByNombreIgnoreCase("CIENCIA")).thenReturn(false);
        when(tematicaRepository.save(any(Tematica.class))).thenAnswer(inv -> inv.getArgument(0));
        assertEquals("CIENCIA", service.actualizarTematica(10L, "CIENCIA").getNombre());
    }

    @Test
    void actualizarTematica_mismoNombre() {
        when(tematicaRepository.findById(10L)).thenReturn(Optional.of(tematica));
        when(tematicaRepository.save(any(Tematica.class))).thenAnswer(inv -> inv.getArgument(0));
        assertEquals("historia", service.actualizarTematica(10L, "historia").getNombre());
    }

    @Test
    void actualizarTematica_nombreDuplicado() {
        when(tematicaRepository.findById(10L)).thenReturn(Optional.of(tematica));
        when(tematicaRepository.existsByNombreIgnoreCase("OTRA")).thenReturn(true);
        assertThrows(IllegalArgumentException.class, () -> service.actualizarTematica(10L, "OTRA"));
    }

    @Test
    void actualizarTematica_noEncontrada() {
        when(tematicaRepository.findById(99L)).thenReturn(Optional.empty());
        assertNull(service.actualizarTematica(99L, "X"));
    }

    @Test
    void eliminarPorId_noExiste() {
        when(tematicaRepository.findById(1L)).thenReturn(Optional.empty());
        assertFalse(service.eliminarTematica(1L));
    }

    @Test
    void eliminarPorId_enUso() {
        when(tematicaRepository.findById(10L)).thenReturn(Optional.of(tematica));
        when(cuestionarioRepository.countByTematicaIgnoreCase("HISTORIA")).thenReturn(1L);
        when(comboRepository.countByTematicaIgnoreCase("HISTORIA")).thenReturn(0L);
        assertThrows(IllegalStateException.class, () -> service.eliminarTematica(10L));
    }

    @Test
    void eliminarPorId_ok() {
        when(tematicaRepository.findById(10L)).thenReturn(Optional.of(tematica));
        when(cuestionarioRepository.countByTematicaIgnoreCase("HISTORIA")).thenReturn(0L);
        when(comboRepository.countByTematicaIgnoreCase("HISTORIA")).thenReturn(0L);
        assertTrue(service.eliminarTematica(10L));
        verify(tematicaRepository).deleteById(10L);
    }

    @Test
    void eliminarPorNombre_noExisteEnUsoYOk() {
        when(tematicaRepository.findByNombreIgnoreCase("NO")).thenReturn(Optional.empty());
        assertFalse(service.eliminarTematica("NO"));

        when(tematicaRepository.findByNombreIgnoreCase("HISTORIA")).thenReturn(Optional.of(tematica));
        when(cuestionarioRepository.countByTematicaIgnoreCase("HISTORIA")).thenReturn(0L);
        when(comboRepository.countByTematicaIgnoreCase("HISTORIA")).thenReturn(2L);
        assertThrows(IllegalStateException.class, () -> service.eliminarTematica("HISTORIA"));

        when(comboRepository.countByTematicaIgnoreCase("HISTORIA")).thenReturn(0L);
        assertTrue(service.eliminarTematica("HISTORIA"));
        verify(tematicaRepository).delete(tematica);
    }

    @Test
    void obtenerNombresYEstadisticas() {
        when(tematicaRepository.findAllByOrderByNombreAsc()).thenReturn(Arrays.asList(tematica));
        List<String> nombres = service.obtenerNombresTematicas();
        assertEquals(Arrays.asList("HISTORIA"), nombres);

        Map<String, Object> stats = service.obtenerEstadisticas();
        assertEquals(1, stats.get("totalTematicas"));
        assertEquals(Arrays.asList(tematica), stats.get("tematicas"));
    }
}
