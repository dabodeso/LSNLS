package com.lsnls.controller;

import com.lsnls.dto.ApiResponse;
import com.lsnls.dto.CrearComboDTO;
import com.lsnls.entity.AuditLog;
import com.lsnls.entity.Combo;
import com.lsnls.entity.Usuario;
import com.lsnls.repository.ComboRepository;
import com.lsnls.service.AuthorizationService;
import com.lsnls.service.ComboService;
import com.lsnls.service.EditLockService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ComboControllerTest {

    @Mock
    private ComboService comboService;
    @Mock
    private AuthorizationService authService;
    @Mock
    private ComboRepository comboRepository;
    @Mock
    private EditLockService editLockService;

    @InjectMocks
    private ComboController comboController;

    private Usuario usuario() {
        Usuario u = new Usuario();
        u.setId(1L);
        u.setNombre("guion");
        u.setRol(Usuario.RolUsuario.ROLE_GUION);
        return u;
    }

    private Combo combo(Long id, Combo.EstadoCombo estado) {
        Combo c = new Combo();
        c.setId(id);
        c.setEstado(estado);
        c.setNivel(Combo.NivelCombo.NORMAL);
        c.setFechaCreacion(LocalDateTime.now());
        return c;
    }

    private CrearComboDTO.PreguntaMultiplicadoraDTO pm(Long id, String factor) {
        CrearComboDTO.PreguntaMultiplicadoraDTO dto = new CrearComboDTO.PreguntaMultiplicadoraDTO();
        dto.setId(id);
        dto.setFactor(factor);
        return dto;
    }

    private CrearComboDTO crearDto(String tipo, String estado, CrearComboDTO.PreguntaMultiplicadoraDTO... pms) {
        CrearComboDTO dto = new CrearComboDTO();
        dto.setTipo(tipo);
        dto.setEstado(estado);
        dto.setPreguntasMultiplicadoras(Arrays.asList(pms));
        return dto;
    }

    @Test
    void obtenerTodos_ok_devuelve200() {
        Map<String, Object> page = Collections.singletonMap("total", 1);
        when(comboService.obtenerTodosPaginados(0, 25)).thenReturn(page);

        ResponseEntity<Map<String, Object>> response = comboController.obtenerTodos(0, 25);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(page, response.getBody());
    }

    @Test
    void obtenerTodos_excepcion_devuelve500() {
        when(comboService.obtenerTodosPaginados(0, 25)).thenThrow(new RuntimeException("fail"));

        ResponseEntity<Map<String, Object>> response = comboController.obtenerTodos(0, 25);

        assertEquals(500, response.getStatusCodeValue());
        assertNull(response.getBody());
    }

    @Test
    void obtenerPorId_ok_devuelve200() {
        Map<String, Object> dto = Collections.singletonMap("id", 1L);
        when(comboService.obtenerComboConSlots(1L)).thenReturn(dto);

        ResponseEntity<Map<String, Object>> response = comboController.obtenerPorId(1L);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(dto, response.getBody());
    }

    @Test
    void obtenerPorId_noEncontrado_devuelve404() {
        when(comboService.obtenerComboConSlots(9L)).thenReturn(null);

        ResponseEntity<Map<String, Object>> response = comboController.obtenerPorId(9L);

        assertEquals(404, response.getStatusCodeValue());
    }

    @Test
    void obtenerPorId_excepcion_devuelve500() {
        when(comboService.obtenerComboConSlots(1L)).thenThrow(new RuntimeException("fail"));

        ResponseEntity<Map<String, Object>> response = comboController.obtenerPorId(1L);

        assertEquals(500, response.getStatusCodeValue());
    }

    @Test
    void obtenerPreguntas_ok_devuelve200() {
        when(comboService.obtenerPreguntasCombo(1L)).thenReturn(Collections.emptyList());

        ResponseEntity<ApiResponse<List<Map<String, Object>>>> response = comboController.obtenerPreguntas(1L);

        assertEquals(200, response.getStatusCodeValue());
        assertTrue(response.getBody().isExito());
    }

    @Test
    void obtenerPreguntas_illegalArgument_devuelve400() {
        when(comboService.obtenerPreguntasCombo(1L)).thenThrow(new IllegalArgumentException("no"));

        ResponseEntity<ApiResponse<List<Map<String, Object>>>> response = comboController.obtenerPreguntas(1L);

        assertEquals(400, response.getStatusCodeValue());
    }

    @Test
    void obtenerPreguntas_excepcion_devuelve500() {
        when(comboService.obtenerPreguntasCombo(1L)).thenThrow(new RuntimeException("fail"));

        ResponseEntity<ApiResponse<List<Map<String, Object>>>> response = comboController.obtenerPreguntas(1L);

        assertEquals(500, response.getStatusCodeValue());
    }

    @Test
    void crearCombo_sinPermiso_devuelve403() {
        when(authService.canCreateCuestionario()).thenReturn(false);

        ResponseEntity<?> response = comboController.crearCombo(new CrearComboDTO());

        assertEquals(403, response.getStatusCodeValue());
    }

    @Test
    void crearCombo_noAutenticado_devuelve401() {
        when(authService.canCreateCuestionario()).thenReturn(true);
        when(authService.getCurrentUser()).thenReturn(Optional.empty());

        ResponseEntity<?> response = comboController.crearCombo(new CrearComboDTO());

        assertEquals(401, response.getStatusCodeValue());
    }

    @Test
    void crearCombo_sinPreguntas_devuelve400() {
        when(authService.canCreateCuestionario()).thenReturn(true);
        when(authService.getCurrentUser()).thenReturn(Optional.of(usuario()));
        CrearComboDTO dto = new CrearComboDTO();
        dto.setPreguntasMultiplicadoras(Collections.emptyList());

        ResponseEntity<?> response = comboController.crearCombo(dto);

        assertEquals(400, response.getStatusCodeValue());
        assertTrue(response.getBody().toString().contains("preguntas multiplicadoras"));
    }

    @Test
    void crearCombo_aprobadoSinTresPreguntas_devuelve400() {
        when(authService.canCreateCuestionario()).thenReturn(true);
        when(authService.getCurrentUser()).thenReturn(Optional.of(usuario()));
        CrearComboDTO dto = crearDto("P", "aprobado", pm(1L, "X2"));

        ResponseEntity<?> response = comboController.crearCombo(dto);

        assertEquals(400, response.getStatusCodeValue());
        assertTrue(response.getBody().toString().contains("exactamente 3"));
    }

    @Test
    void crearCombo_tipoVacio_devuelve400() {
        when(authService.canCreateCuestionario()).thenReturn(true);
        when(authService.getCurrentUser()).thenReturn(Optional.of(usuario()));
        CrearComboDTO dto = crearDto("  ", "borrador", pm(1L, "X2"));

        ResponseEntity<?> response = comboController.crearCombo(dto);

        assertEquals(400, response.getStatusCodeValue());
        assertTrue(response.getBody().toString().contains("tipo"));
    }

    @Test
    void crearCombo_preguntaSinId_devuelve400() {
        when(authService.canCreateCuestionario()).thenReturn(true);
        when(authService.getCurrentUser()).thenReturn(Optional.of(usuario()));
        CrearComboDTO dto = crearDto("P", "borrador", pm(null, "X2"));

        ResponseEntity<?> response = comboController.crearCombo(dto);

        assertEquals(400, response.getStatusCodeValue());
        assertTrue(response.getBody().toString().contains("ID válido"));
    }

    @Test
    void crearCombo_preguntaSinFactor_devuelve400() {
        when(authService.canCreateCuestionario()).thenReturn(true);
        when(authService.getCurrentUser()).thenReturn(Optional.of(usuario()));
        CrearComboDTO dto = crearDto("P", "borrador", pm(1L, " "));

        ResponseEntity<?> response = comboController.crearCombo(dto);

        assertEquals(400, response.getStatusCodeValue());
        assertTrue(response.getBody().toString().contains("factor"));
    }

    @Test
    void crearCombo_idsDuplicados_devuelve400() {
        when(authService.canCreateCuestionario()).thenReturn(true);
        when(authService.getCurrentUser()).thenReturn(Optional.of(usuario()));
        CrearComboDTO dto = crearDto("P", "borrador", pm(1L, "X2"), pm(1L, "X3"));

        ResponseEntity<?> response = comboController.crearCombo(dto);

        assertEquals(400, response.getStatusCodeValue());
        assertTrue(response.getBody().toString().contains("misma pregunta"));
    }

    @Test
    void crearCombo_tipoInvalido_devuelve400() {
        when(authService.canCreateCuestionario()).thenReturn(true);
        when(authService.getCurrentUser()).thenReturn(Optional.of(usuario()));
        CrearComboDTO dto = crearDto("Z", "borrador", pm(1L, "X2"));

        ResponseEntity<?> response = comboController.crearCombo(dto);

        assertEquals(400, response.getStatusCodeValue());
        assertTrue(response.getBody().toString().contains("no válido"));
    }

    @Test
    void crearCombo_ok_devuelve200() {
        when(authService.canCreateCuestionario()).thenReturn(true);
        when(authService.getCurrentUser()).thenReturn(Optional.of(usuario()));
        CrearComboDTO dto = crearDto("P", null, pm(1L, "X2"), pm(2L, "X3"), pm(3L, "X"));
        Combo creado = combo(20L, Combo.EstadoCombo.borrador);
        when(comboService.crearComboDesdeDTO(any(CrearComboDTO.class), any(Usuario.class))).thenReturn(creado);

        ResponseEntity<?> response = comboController.crearCombo(dto);

        assertEquals(200, response.getStatusCodeValue());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals(20L, body.get("id"));
    }

    @Test
    void crearCombo_concurrencia_devuelve409() {
        when(authService.canCreateCuestionario()).thenReturn(true);
        when(authService.getCurrentUser()).thenReturn(Optional.of(usuario()));
        CrearComboDTO dto = crearDto("P", "borrador", pm(1L, "X2"));
        when(comboService.crearComboDesdeDTO(any(), any()))
                .thenThrow(new IllegalArgumentException("Error de concurrencia al crear"));

        ResponseEntity<?> response = comboController.crearCombo(dto);

        assertEquals(409, response.getStatusCodeValue());
    }

    @Test
    void crearCombo_illegalArgument_devuelve400() {
        when(authService.canCreateCuestionario()).thenReturn(true);
        when(authService.getCurrentUser()).thenReturn(Optional.of(usuario()));
        CrearComboDTO dto = crearDto("P", "borrador", pm(1L, "X2"));
        when(comboService.crearComboDesdeDTO(any(), any())).thenThrow(new IllegalArgumentException("incompleto"));

        ResponseEntity<?> response = comboController.crearCombo(dto);

        assertEquals(400, response.getStatusCodeValue());
        assertEquals("incompleto", response.getBody());
    }

    @Test
    void crearCombo_runtime_devuelve400() {
        when(authService.canCreateCuestionario()).thenReturn(true);
        when(authService.getCurrentUser()).thenReturn(Optional.of(usuario()));
        CrearComboDTO dto = crearDto("P", "borrador", pm(1L, "X2"));
        when(comboService.crearComboDesdeDTO(any(), any())).thenThrow(new RuntimeException("fail"));

        ResponseEntity<?> response = comboController.crearCombo(dto);

        assertEquals(400, response.getStatusCodeValue());
        assertTrue(response.getBody().toString().contains("Error al crear combo"));
    }

    @Test
    void agregarPregunta_sinPermiso_devuelve403() {
        when(authService.canCreateCuestionario()).thenReturn(false);

        ResponseEntity<?> response = comboController.agregarPregunta(1L, Collections.singletonMap("preguntaId", 2));

        assertEquals(403, response.getStatusCodeValue());
    }

    @Test
    void agregarPregunta_asignadoAJornada_devuelve409() {
        when(authService.canCreateCuestionario()).thenReturn(true);
        when(comboService.estaAsignadoAJornada(1L)).thenReturn(true);

        ResponseEntity<?> response = comboController.agregarPregunta(1L, Collections.singletonMap("preguntaId", 2));

        assertEquals(409, response.getStatusCodeValue());
    }

    @Test
    void agregarPregunta_ok_devuelve200() {
        when(authService.canCreateCuestionario()).thenReturn(true);
        when(comboService.estaAsignadoAJornada(1L)).thenReturn(false);
        when(comboService.agregarPregunta(1L, 2L, 3, 1)).thenReturn(true);
        Map<String, Object> request = new HashMap<>();
        request.put("preguntaId", 2);
        request.put("factorMultiplicacion", 3);
        request.put("posicion", 1);

        ResponseEntity<?> response = comboController.agregarPregunta(1L, request);

        assertEquals(200, response.getStatusCodeValue());
    }

    @Test
    void agregarPregunta_fallo_devuelve400() {
        when(authService.canCreateCuestionario()).thenReturn(true);
        when(comboService.estaAsignadoAJornada(1L)).thenReturn(false);
        when(comboService.agregarPregunta(1L, 2L, 1, null)).thenReturn(false);

        ResponseEntity<?> response = comboController.agregarPregunta(1L, Collections.singletonMap("preguntaId", 2));

        assertEquals(400, response.getStatusCodeValue());
    }

    @Test
    void agregarPregunta_runtime_devuelve400() {
        when(authService.canCreateCuestionario()).thenReturn(true);
        when(comboService.estaAsignadoAJornada(1L)).thenReturn(false);
        when(comboService.agregarPregunta(1L, 2L, 1, null)).thenThrow(new RuntimeException("fail"));

        ResponseEntity<?> response = comboController.agregarPregunta(1L, Collections.singletonMap("preguntaId", 2));

        assertEquals(400, response.getStatusCodeValue());
    }

    @Test
    void quitarPregunta_sinPermiso_devuelve403() {
        when(authService.canCreateCuestionario()).thenReturn(false);

        ResponseEntity<?> response = comboController.quitarPregunta(1L, 2L);

        assertEquals(403, response.getStatusCodeValue());
    }

    @Test
    void quitarPregunta_asignadoAJornada_devuelve409() {
        when(authService.canCreateCuestionario()).thenReturn(true);
        when(comboService.estaAsignadoAJornada(1L)).thenReturn(true);

        ResponseEntity<?> response = comboController.quitarPregunta(1L, 2L);

        assertEquals(409, response.getStatusCodeValue());
    }

    @Test
    void quitarPregunta_ok_devuelve200() {
        when(authService.canCreateCuestionario()).thenReturn(true);
        when(comboService.estaAsignadoAJornada(1L)).thenReturn(false);
        when(comboService.quitarPregunta(1L, 2L)).thenReturn(true);

        ResponseEntity<?> response = comboController.quitarPregunta(1L, 2L);

        assertEquals(200, response.getStatusCodeValue());
    }

    @Test
    void quitarPregunta_fallo_devuelve400() {
        when(authService.canCreateCuestionario()).thenReturn(true);
        when(comboService.estaAsignadoAJornada(1L)).thenReturn(false);
        when(comboService.quitarPregunta(1L, 2L)).thenReturn(false);

        ResponseEntity<?> response = comboController.quitarPregunta(1L, 2L);

        assertEquals(400, response.getStatusCodeValue());
    }

    @Test
    void quitarPregunta_excepcion_devuelve500() {
        when(authService.canCreateCuestionario()).thenReturn(true);
        when(comboService.estaAsignadoAJornada(1L)).thenReturn(false);
        when(comboService.quitarPregunta(1L, 2L)).thenThrow(new RuntimeException("fail"));

        ResponseEntity<?> response = comboController.quitarPregunta(1L, 2L);

        assertEquals(500, response.getStatusCodeValue());
    }

    @Test
    void limpiarPreguntasInvalidas_sinPermiso_devuelve403() {
        when(authService.canCreateCuestionario()).thenReturn(false);

        ResponseEntity<?> response = comboController.limpiarPreguntasInvalidas(1L);

        assertEquals(403, response.getStatusCodeValue());
    }

    @Test
    void limpiarPreguntasInvalidas_ok_devuelve200() {
        when(authService.canCreateCuestionario()).thenReturn(true);
        when(comboService.limpiarPreguntasInvalidas(1L)).thenReturn(2);

        ResponseEntity<?> response = comboController.limpiarPreguntasInvalidas(1L);

        assertEquals(200, response.getStatusCodeValue());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals(2, body.get("preguntasEliminadas"));
    }

    @Test
    void limpiarPreguntasInvalidas_excepcion_devuelve500() {
        when(authService.canCreateCuestionario()).thenReturn(true);
        when(comboService.limpiarPreguntasInvalidas(1L)).thenThrow(new RuntimeException("fail"));

        ResponseEntity<?> response = comboController.limpiarPreguntasInvalidas(1L);

        assertEquals(500, response.getStatusCodeValue());
    }

    @Test
    void obtenerCombosParaAsignar_ok_devuelve200() {
        when(comboService.obtenerDisponiblesParaConcursantes())
                .thenReturn(Collections.singletonList(combo(1L, Combo.EstadoCombo.aprobado)));

        ResponseEntity<List<Map<String, Object>>> response = comboController.obtenerCombosParaAsignar();

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(1, response.getBody().size());
        assertEquals(1L, response.getBody().get(0).get("id"));
    }

    @Test
    void obtenerCombosParaAsignar_excepcion_devuelve500() {
        when(comboService.obtenerDisponiblesParaConcursantes()).thenThrow(new RuntimeException("fail"));

        ResponseEntity<List<Map<String, Object>>> response = comboController.obtenerCombosParaAsignar();

        assertEquals(500, response.getStatusCodeValue());
    }

    @Test
    void obtenerPorEstado_ok_devuelve200() {
        when(comboService.obtenerPorEstado(Combo.EstadoCombo.borrador)).thenReturn(Collections.emptyList());

        ResponseEntity<List<Combo>> response = comboController.obtenerPorEstado(Combo.EstadoCombo.borrador);

        assertEquals(200, response.getStatusCodeValue());
    }

    @Test
    void obtenerPorEstado_excepcion_devuelve500() {
        when(comboService.obtenerPorEstado(Combo.EstadoCombo.borrador)).thenThrow(new RuntimeException("fail"));

        ResponseEntity<List<Combo>> response = comboController.obtenerPorEstado(Combo.EstadoCombo.borrador);

        assertEquals(500, response.getStatusCodeValue());
    }

    @Test
    void obtenerPorNivel_ok_devuelve200() {
        when(comboService.obtenerPorNivel(Combo.NivelCombo.NORMAL)).thenReturn(Collections.emptyList());

        ResponseEntity<List<Combo>> response = comboController.obtenerPorNivel(Combo.NivelCombo.NORMAL);

        assertEquals(200, response.getStatusCodeValue());
    }

    @Test
    void obtenerPorNivel_excepcion_devuelve500() {
        when(comboService.obtenerPorNivel(Combo.NivelCombo.NORMAL)).thenThrow(new RuntimeException("fail"));

        ResponseEntity<List<Combo>> response = comboController.obtenerPorNivel(Combo.NivelCombo.NORMAL);

        assertEquals(500, response.getStatusCodeValue());
    }

    @Test
    void filtrarCombos_porId_devuelve200() {
        Map<String, Object> page = Collections.singletonMap("total", 1);
        when(comboService.filtrarCombosPorId("5", 0, 25)).thenReturn(page);

        ResponseEntity<Map<String, Object>> response =
                comboController.filtrarCombos(null, null, null, null, "5", null, 0, 25);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(page, response.getBody());
    }

    @Test
    void filtrarCombos_porFiltros_devuelve200() {
        Map<String, Object> page = Collections.singletonMap("total", 0);
        when(comboService.filtrarCombos("borrador", "P", "HISTORIA", null, "texto", 0, 25)).thenReturn(page);

        ResponseEntity<Map<String, Object>> response =
                comboController.filtrarCombos("borrador", "P", "HISTORIA", null, null, "texto", 0, 25);

        assertEquals(200, response.getStatusCodeValue());
    }

    @Test
    void filtrarCombos_excepcion_devuelve500() {
        when(comboService.filtrarCombos(null, null, null, null, null, 0, 25)).thenThrow(new RuntimeException("fail"));

        ResponseEntity<Map<String, Object>> response =
                comboController.filtrarCombos(null, null, null, null, "", null, 0, 25);

        assertEquals(500, response.getStatusCodeValue());
    }

    @Test
    void cambiarEstado_noEncontrado_devuelve404() {
        when(comboRepository.findById(9L)).thenReturn(Optional.empty());

        ResponseEntity<?> response = comboController.cambiarEstado(9L, Combo.EstadoCombo.aprobado);

        assertEquals(404, response.getStatusCodeValue());
    }

    @Test
    void cambiarEstado_asignadoAJornada_devuelve409() {
        when(comboRepository.findById(1L)).thenReturn(Optional.of(combo(1L, Combo.EstadoCombo.adjudicado)));
        when(comboService.estaAsignadoAJornada(1L)).thenReturn(true);

        ResponseEntity<?> response = comboController.cambiarEstado(1L, Combo.EstadoCombo.aprobado);

        assertEquals(409, response.getStatusCodeValue());
    }

    @Test
    void cambiarEstado_sinPermiso_devuelve403() {
        Combo c = combo(1L, Combo.EstadoCombo.aprobado);
        when(comboRepository.findById(1L)).thenReturn(Optional.of(c));
        when(comboService.estaAsignadoAJornada(1L)).thenReturn(false);
        when(authService.canEditCombo(Combo.EstadoCombo.aprobado)).thenReturn(false);

        ResponseEntity<?> response = comboController.cambiarEstado(1L, Combo.EstadoCombo.borrador);

        assertEquals(403, response.getStatusCodeValue());
    }

    @Test
    void cambiarEstado_ok_devuelve200() {
        Combo c = combo(1L, Combo.EstadoCombo.borrador);
        when(comboRepository.findById(1L)).thenReturn(Optional.of(c));
        when(comboService.estaAsignadoAJornada(1L)).thenReturn(false);
        when(authService.canEditCombo(Combo.EstadoCombo.borrador)).thenReturn(true);
        when(authService.isAdmin()).thenReturn(true);
        when(comboService.cambiarEstado(1L, Combo.EstadoCombo.aprobado, true)).thenReturn(c);

        ResponseEntity<?> response = comboController.cambiarEstado(1L, Combo.EstadoCombo.aprobado);

        assertEquals(200, response.getStatusCodeValue());
    }

    @Test
    void cambiarEstado_servicioDevuelveNull_devuelve400() {
        Combo c = combo(1L, Combo.EstadoCombo.borrador);
        when(comboRepository.findById(1L)).thenReturn(Optional.of(c));
        when(comboService.estaAsignadoAJornada(1L)).thenReturn(false);
        when(authService.canEditCombo(Combo.EstadoCombo.borrador)).thenReturn(true);
        when(authService.isAdmin()).thenReturn(false);
        when(comboService.cambiarEstado(1L, Combo.EstadoCombo.revisar, false)).thenReturn(null);

        ResponseEntity<?> response = comboController.cambiarEstado(1L, Combo.EstadoCombo.revisar);

        assertEquals(400, response.getStatusCodeValue());
    }

    @Test
    void cambiarEstado_illegalArgument_devuelve400() {
        Combo c = combo(1L, Combo.EstadoCombo.borrador);
        when(comboRepository.findById(1L)).thenReturn(Optional.of(c));
        when(comboService.estaAsignadoAJornada(1L)).thenReturn(false);
        when(authService.canEditCombo(Combo.EstadoCombo.borrador)).thenReturn(true);
        when(authService.isAdmin()).thenReturn(false);
        when(comboService.cambiarEstado(eq(1L), eq(Combo.EstadoCombo.aprobado), anyBoolean()))
                .thenThrow(new IllegalArgumentException("transición inválida"));

        ResponseEntity<?> response = comboController.cambiarEstado(1L, Combo.EstadoCombo.aprobado);

        assertEquals(400, response.getStatusCodeValue());
        assertEquals("transición inválida", response.getBody());
    }

    @Test
    void cambiarEstado_excepcion_devuelve500() {
        when(comboRepository.findById(1L)).thenThrow(new RuntimeException("fail"));

        ResponseEntity<?> response = comboController.cambiarEstado(1L, Combo.EstadoCombo.aprobado);

        assertEquals(500, response.getStatusCodeValue());
    }

    @Test
    void actualizarCombo_sinPermiso_devuelve403() {
        when(authService.canCreateCuestionario()).thenReturn(false);

        ResponseEntity<?> response = comboController.actualizarCombo(1L, new HashMap<>());

        assertEquals(403, response.getStatusCodeValue());
    }

    @Test
    void actualizarCombo_noEncontrado_devuelve400() {
        when(authService.canCreateCuestionario()).thenReturn(true);
        when(comboService.obtenerPorId(9L)).thenReturn(Optional.empty());

        ResponseEntity<?> response = comboController.actualizarCombo(9L, new HashMap<>());

        assertEquals(400, response.getStatusCodeValue());
        assertTrue(response.getBody().toString().contains("no encontrado"));
    }

    @Test
    void actualizarCombo_tipoInvalido_devuelve400() {
        when(authService.canCreateCuestionario()).thenReturn(true);
        when(comboService.obtenerPorId(1L)).thenReturn(Optional.of(combo(1L, Combo.EstadoCombo.borrador)));
        Map<String, Object> datos = new HashMap<>();
        datos.put("tipo", "Z");

        ResponseEntity<?> response = comboController.actualizarCombo(1L, datos);

        assertEquals(400, response.getStatusCodeValue());
        assertTrue(response.getBody().toString().contains("Tipo de combo inválido"));
    }

    @Test
    void actualizarCombo_estadoInvalido_devuelve400() {
        when(authService.canCreateCuestionario()).thenReturn(true);
        when(comboService.obtenerPorId(1L)).thenReturn(Optional.of(combo(1L, Combo.EstadoCombo.borrador)));
        Map<String, Object> datos = new HashMap<>();
        datos.put("estado", "NO_EXISTE");

        ResponseEntity<?> response = comboController.actualizarCombo(1L, datos);

        assertEquals(400, response.getStatusCodeValue());
        assertTrue(response.getBody().toString().contains("Estado de combo inválido"));
    }

    @Test
    void actualizarCombo_ok_devuelve200() {
        Combo c = combo(1L, Combo.EstadoCombo.borrador);
        when(authService.canCreateCuestionario()).thenReturn(true);
        when(comboService.obtenerPorId(1L)).thenReturn(Optional.of(c));
        when(comboService.actualizar(eq(1L), any(Combo.class))).thenReturn(c);
        Map<String, Object> datos = new HashMap<>();
        datos.put("version", 2);
        datos.put("tipo", "A");
        datos.put("tematica", "HISTORIA");
        datos.put("notasDireccion", "ok");
        datos.put("estado", "borrador");
        doNothing().when(comboService).validarTransicionEstado(any(), any(), anyBoolean());

        ResponseEntity<?> response = comboController.actualizarCombo(1L, datos);

        assertEquals(200, response.getStatusCodeValue());
    }

    @Test
    void actualizarCombo_aprobadoConListaIncompleta_devuelve400() {
        Combo c = combo(1L, Combo.EstadoCombo.borrador);
        when(authService.canCreateCuestionario()).thenReturn(true);
        when(comboService.obtenerPorId(1L)).thenReturn(Optional.of(c));
        when(authService.isAdmin()).thenReturn(false);
        doNothing().when(comboService).validarTransicionEstado(any(), any(), anyBoolean());
        Map<String, Object> datos = new HashMap<>();
        datos.put("estado", "aprobado");
        datos.put("preguntasMultiplicadoras", Collections.singletonList(1));

        ResponseEntity<?> response = comboController.actualizarCombo(1L, datos);

        assertEquals(400, response.getStatusCodeValue());
        assertTrue(response.getBody().toString().contains("exactamente 3"));
    }

    @Test
    void actualizarCombo_servicioDevuelveNull_devuelve400() {
        Combo c = combo(1L, Combo.EstadoCombo.borrador);
        when(authService.canCreateCuestionario()).thenReturn(true);
        when(comboService.obtenerPorId(1L)).thenReturn(Optional.of(c));
        when(comboService.actualizar(eq(1L), any(Combo.class))).thenReturn(null);

        ResponseEntity<?> response = comboController.actualizarCombo(1L, new HashMap<>());

        assertEquals(400, response.getStatusCodeValue());
        assertTrue(response.getBody().toString().contains("Error al actualizar combo"));
    }

    @Test
    void actualizarCombo_optimisticLock_devuelve409() {
        Combo c = combo(1L, Combo.EstadoCombo.borrador);
        when(authService.canCreateCuestionario()).thenReturn(true);
        when(comboService.obtenerPorId(1L)).thenReturn(Optional.of(c));
        when(comboService.actualizar(eq(1L), any(Combo.class)))
                .thenThrow(new ObjectOptimisticLockingFailureException("conflicto", new RuntimeException()));

        ResponseEntity<?> response = comboController.actualizarCombo(1L, new HashMap<>());

        assertEquals(409, response.getStatusCodeValue());
    }

    @Test
    void actualizarFactorPregunta_sinPermiso_devuelve403() {
        when(authService.canCreateCuestionario()).thenReturn(false);

        ResponseEntity<?> response = comboController.actualizarFactorPregunta(1L, 2L, new HashMap<>());

        assertEquals(403, response.getStatusCodeValue());
    }

    @Test
    void actualizarFactorPregunta_sinParametro_devuelve400() {
        when(authService.canCreateCuestionario()).thenReturn(true);

        ResponseEntity<?> response = comboController.actualizarFactorPregunta(1L, 2L, new HashMap<>());

        assertEquals(400, response.getStatusCodeValue());
        assertTrue(response.getBody().toString().contains("factorMultiplicacion"));
    }

    @Test
    void actualizarFactorPregunta_ok_devuelve200() {
        when(authService.canCreateCuestionario()).thenReturn(true);
        when(comboService.actualizarFactorPregunta(1L, 2L, "X2")).thenReturn(true);
        Map<String, Object> request = new HashMap<>();
        request.put("factorMultiplicacion", "X2");

        ResponseEntity<?> response = comboController.actualizarFactorPregunta(1L, 2L, request);

        assertEquals(200, response.getStatusCodeValue());
    }

    @Test
    void actualizarFactorPregunta_fallo_devuelve400() {
        when(authService.canCreateCuestionario()).thenReturn(true);
        when(comboService.actualizarFactorPregunta(1L, 2L, "X2")).thenReturn(false);
        Map<String, Object> request = new HashMap<>();
        request.put("factorMultiplicacion", "X2");

        ResponseEntity<?> response = comboController.actualizarFactorPregunta(1L, 2L, request);

        assertEquals(400, response.getStatusCodeValue());
    }

    @Test
    void actualizarFactorPregunta_excepcion_devuelve500() {
        when(authService.canCreateCuestionario()).thenReturn(true);
        when(comboService.actualizarFactorPregunta(1L, 2L, "X2")).thenThrow(new RuntimeException("fail"));
        Map<String, Object> request = new HashMap<>();
        request.put("factorMultiplicacion", "X2");

        ResponseEntity<?> response = comboController.actualizarFactorPregunta(1L, 2L, request);

        assertEquals(500, response.getStatusCodeValue());
    }

    @Test
    void eliminar_sinPermiso_devuelve403() {
        when(authService.canDelete()).thenReturn(false);

        ResponseEntity<?> response = comboController.eliminar(1L);

        assertEquals(403, response.getStatusCodeValue());
    }

    @Test
    void eliminar_noEncontrado_devuelve404() {
        when(authService.canDelete()).thenReturn(true);
        when(comboService.obtenerPorId(9L)).thenReturn(Optional.empty());

        ResponseEntity<?> response = comboController.eliminar(9L);

        assertEquals(404, response.getStatusCodeValue());
    }

    @Test
    void eliminar_adjudicado_devuelve400() {
        when(authService.canDelete()).thenReturn(true);
        when(comboService.obtenerPorId(1L)).thenReturn(Optional.of(combo(1L, Combo.EstadoCombo.adjudicado)));

        ResponseEntity<?> response = comboController.eliminar(1L);

        assertEquals(400, response.getStatusCodeValue());
        assertTrue(response.getBody().toString().contains("adjudicado"));
    }

    @Test
    void eliminar_grabado_devuelve400() {
        when(authService.canDelete()).thenReturn(true);
        when(comboService.obtenerPorId(1L)).thenReturn(Optional.of(combo(1L, Combo.EstadoCombo.grabado)));

        ResponseEntity<?> response = comboController.eliminar(1L);

        assertEquals(400, response.getStatusCodeValue());
        assertTrue(response.getBody().toString().contains("grabado"));
    }

    @Test
    void eliminar_ok_devuelve200() {
        when(authService.canDelete()).thenReturn(true);
        when(comboService.obtenerPorId(1L)).thenReturn(Optional.of(combo(1L, Combo.EstadoCombo.borrador)));
        when(authService.getCurrentUser()).thenReturn(Optional.of(usuario()));
        doNothing().when(comboService).eliminar(1L);

        ResponseEntity<?> response = comboController.eliminar(1L);

        assertEquals(200, response.getStatusCodeValue());
    }

    @Test
    void eliminar_illegalArgument_devuelve400() {
        when(authService.canDelete()).thenReturn(true);
        when(comboService.obtenerPorId(1L)).thenReturn(Optional.of(combo(1L, Combo.EstadoCombo.borrador)));
        doThrow(new IllegalArgumentException("no")).when(comboService).eliminar(1L);

        ResponseEntity<?> response = comboController.eliminar(1L);

        assertEquals(400, response.getStatusCodeValue());
        assertEquals("no", response.getBody());
    }

    @Test
    void eliminar_foreignKey_devuelve400() {
        when(authService.canDelete()).thenReturn(true);
        when(comboService.obtenerPorId(1L)).thenReturn(Optional.of(combo(1L, Combo.EstadoCombo.borrador)));
        doThrow(new RuntimeException("foreign key constraint")).when(comboService).eliminar(1L);

        ResponseEntity<?> response = comboController.eliminar(1L);

        assertEquals(400, response.getStatusCodeValue());
        assertTrue(response.getBody().toString().contains("concursantes o jornadas"));
    }

    @Test
    void eliminar_mensajeJornada_devuelve400() {
        when(authService.canDelete()).thenReturn(true);
        when(comboService.obtenerPorId(1L)).thenReturn(Optional.of(combo(1L, Combo.EstadoCombo.borrador)));
        doThrow(new RuntimeException("asignado a jornada")).when(comboService).eliminar(1L);

        ResponseEntity<?> response = comboController.eliminar(1L);

        assertEquals(400, response.getStatusCodeValue());
        assertTrue(response.getBody().toString().contains("asignado a una jornada"));
    }

    @Test
    void eliminar_mensajeConcursante_devuelve400() {
        when(authService.canDelete()).thenReturn(true);
        when(comboService.obtenerPorId(1L)).thenReturn(Optional.of(combo(1L, Combo.EstadoCombo.borrador)));
        doThrow(new RuntimeException("asignado a concursante")).when(comboService).eliminar(1L);

        ResponseEntity<?> response = comboController.eliminar(1L);

        assertEquals(400, response.getStatusCodeValue());
        assertTrue(response.getBody().toString().contains("asignado a concursantes"));
    }

    @Test
    void eliminar_errorGenerico_devuelve400() {
        when(authService.canDelete()).thenReturn(true);
        when(comboService.obtenerPorId(1L)).thenReturn(Optional.of(combo(1L, Combo.EstadoCombo.borrador)));
        doThrow(new RuntimeException("otro")).when(comboService).eliminar(1L);

        ResponseEntity<?> response = comboController.eliminar(1L);

        assertEquals(400, response.getStatusCodeValue());
        assertTrue(response.getBody().toString().contains("No se pudo eliminar"));
    }
}
