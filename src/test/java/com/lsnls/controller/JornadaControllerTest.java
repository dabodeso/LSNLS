package com.lsnls.controller;

import com.lsnls.dto.ApiResponse;
import com.lsnls.dto.JornadaDTO;
import com.lsnls.dto.ReciclajeComboDTO;
import com.lsnls.entity.AuditLog;
import com.lsnls.entity.Usuario;
import com.lsnls.service.AuthorizationService;
import com.lsnls.service.EditLockService;
import com.lsnls.service.JornadaService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JornadaControllerTest {

    @Mock
    private JornadaService jornadaService;
    @Mock
    private AuthorizationService authService;
    @Mock
    private EditLockService editLockService;

    @InjectMocks
    private JornadaController jornadaController;

    private Usuario usuario(Long id) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setNombre("admin");
        u.setRol(Usuario.RolUsuario.ROLE_ADMIN);
        return u;
    }

    private JornadaDTO jornada(Long id, String nombre) {
        JornadaDTO dto = new JornadaDTO();
        dto.setId(id);
        dto.setNombre(nombre);
        dto.setFechaJornada(LocalDate.of(2026, 1, 15));
        return dto;
    }

    @Test
    void obtenerTodas_ok_devuelve200() {
        Page<JornadaDTO> page = new PageImpl<>(Collections.singletonList(jornada(1L, "J1")));
        when(jornadaService.obtenerTodasPaginadasConFiltros(any(Pageable.class), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(page);

        ResponseEntity<ApiResponse<Page<JornadaDTO>>> response =
                jornadaController.obtenerTodas(0, 10, "id", "desc", null, null, null, null);

        assertEquals(200, response.getStatusCodeValue());
        assertTrue(response.getBody().isExito());
    }

    @Test
    void obtenerTodas_excepcion_devuelve500() {
        when(jornadaService.obtenerTodasPaginadasConFiltros(any(Pageable.class), isNull(), isNull(), isNull(), isNull()))
                .thenThrow(new RuntimeException("fail"));

        ResponseEntity<ApiResponse<Page<JornadaDTO>>> response =
                jornadaController.obtenerTodas(0, 10, "id", "asc", null, null, null, null);

        assertEquals(500, response.getStatusCodeValue());
    }

    @Test
    void obtenerPorId_ok_devuelve200() {
        JornadaDTO dto = jornada(1L, "J1");
        when(jornadaService.obtenerPorId(1L)).thenReturn(Optional.of(dto));

        ResponseEntity<ApiResponse<JornadaDTO>> response = jornadaController.obtenerPorId(1L);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(dto, response.getBody().getDatos());
    }

    @Test
    void obtenerPorId_noEncontrada_devuelve404() {
        when(jornadaService.obtenerPorId(9L)).thenReturn(Optional.empty());

        ResponseEntity<ApiResponse<JornadaDTO>> response = jornadaController.obtenerPorId(9L);

        assertEquals(404, response.getStatusCodeValue());
        assertTrue(response.getBody().getMensaje().contains("no encontrada"));
    }

    @Test
    void obtenerPorId_excepcion_devuelve500() {
        when(jornadaService.obtenerPorId(1L)).thenThrow(new RuntimeException("fail"));

        ResponseEntity<ApiResponse<JornadaDTO>> response = jornadaController.obtenerPorId(1L);

        assertEquals(500, response.getStatusCodeValue());
    }

    @Test
    void obtenerPorId_sqlHibernateNoLlegaAlCliente() {
        when(jornadaService.obtenerPorId(1L)).thenThrow(new RuntimeException(
                "could not execute statement; nested exception is java.sql.SQLException: Duplicate entry"));

        ResponseEntity<ApiResponse<JornadaDTO>> response = jornadaController.obtenerPorId(1L);

        assertEquals(500, response.getStatusCodeValue());
        String mensaje = response.getBody().getMensaje();
        assertFalse(mensaje.toLowerCase().contains("sql"));
        assertFalse(mensaje.toLowerCase().contains("hibernate"));
        assertFalse(mensaje.contains("Duplicate entry"));
    }

    @Test
    void crear_nombreVacio_devuelve400() {
        ResponseEntity<ApiResponse<JornadaDTO>> response = jornadaController.crear(jornada(null, "  "));

        assertEquals(400, response.getStatusCodeValue());
        assertTrue(response.getBody().getMensaje().contains("nombre"));
    }

    @Test
    void crear_demasiadosCuestionarios_devuelve400() {
        JornadaDTO dto = jornada(null, "J1");
        dto.setCuestionarioIds(Arrays.asList(1L, 2L, 3L, 4L, 5L, 6L));

        ResponseEntity<ApiResponse<JornadaDTO>> response = jornadaController.crear(dto);

        assertEquals(400, response.getStatusCodeValue());
        assertTrue(response.getBody().getMensaje().contains("máximo 5 cuestionarios"));
    }

    @Test
    void crear_demasiadosCombos_devuelve400() {
        JornadaDTO dto = jornada(null, "J1");
        dto.setComboIds(Arrays.asList(1L, 2L, 3L, 4L, 5L, 6L));

        ResponseEntity<ApiResponse<JornadaDTO>> response = jornadaController.crear(dto);

        assertEquals(400, response.getStatusCodeValue());
        assertTrue(response.getBody().getMensaje().contains("máximo 5 combos"));
    }

    @Test
    void crear_noAutenticado_devuelve401() {
        when(authService.getCurrentUser()).thenReturn(Optional.empty());

        ResponseEntity<ApiResponse<JornadaDTO>> response = jornadaController.crear(jornada(null, "J1"));

        assertEquals(401, response.getStatusCodeValue());
    }

    @Test
    void crear_ok_devuelve200() {
        JornadaDTO dto = jornada(null, "J1");
        dto.setCuestionarioIds(Collections.singletonList(1L));
        dto.setComboIds(Collections.singletonList(2L));
        JornadaDTO creada = jornada(10L, "J1");
        when(authService.getCurrentUser()).thenReturn(Optional.of(usuario(1L)));
        when(jornadaService.crear(dto, 1L)).thenReturn(creada);

        ResponseEntity<ApiResponse<JornadaDTO>> response = jornadaController.crear(dto);

        assertEquals(200, response.getStatusCodeValue());
        assertTrue(response.getBody().getMensaje().contains("1 cuestionarios"));
        assertTrue(response.getBody().getMensaje().contains("1 combos"));
    }

    @Test
    void crear_illegalArgument_devuelve400() {
        JornadaDTO dto = jornada(null, "J1");
        when(authService.getCurrentUser()).thenReturn(Optional.of(usuario(1L)));
        when(jornadaService.crear(dto, 1L)).thenThrow(new IllegalArgumentException("duplicada"));

        ResponseEntity<ApiResponse<JornadaDTO>> response = jornadaController.crear(dto);

        assertEquals(400, response.getStatusCodeValue());
        assertTrue(response.getBody().getMensaje().contains("Error de validación"));
    }

    @Test
    void crear_excepcion_devuelve500() {
        JornadaDTO dto = jornada(null, "J1");
        when(authService.getCurrentUser()).thenReturn(Optional.of(usuario(1L)));
        when(jornadaService.crear(dto, 1L)).thenThrow(new RuntimeException("fail"));

        ResponseEntity<ApiResponse<JornadaDTO>> response = jornadaController.crear(dto);

        assertEquals(500, response.getStatusCodeValue());
    }

    @Test
    void actualizar_ok_devuelve200() {
        JornadaDTO dto = jornada(1L, "J1");
        doNothing().when(editLockService).assertCanEdit(AuditLog.EntityType.JORNADA, 1L);
        when(jornadaService.actualizar(1L, dto)).thenReturn(dto);
        doNothing().when(editLockService).logEntityUpdate(AuditLog.EntityType.JORNADA, 1L, "Actualización de jornada");

        ResponseEntity<ApiResponse<JornadaDTO>> response = jornadaController.actualizar(1L, dto);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(dto, response.getBody().getDatos());
    }

    @Test
    void actualizar_optimisticLock_devuelve409() {
        JornadaDTO dto = jornada(1L, "J1");
        when(jornadaService.actualizar(1L, dto))
                .thenThrow(new ObjectOptimisticLockingFailureException("conflicto", new RuntimeException()));

        ResponseEntity<ApiResponse<JornadaDTO>> response = jornadaController.actualizar(1L, dto);

        assertEquals(409, response.getStatusCodeValue());
    }

    @Test
    void actualizar_illegalArgument_devuelve400() {
        JornadaDTO dto = jornada(1L, "J1");
        when(jornadaService.actualizar(1L, dto)).thenThrow(new IllegalArgumentException("inválido"));

        ResponseEntity<ApiResponse<JornadaDTO>> response = jornadaController.actualizar(1L, dto);

        assertEquals(400, response.getStatusCodeValue());
    }

    @Test
    void actualizar_excepcion_devuelve500() {
        JornadaDTO dto = jornada(1L, "J1");
        when(jornadaService.actualizar(1L, dto)).thenThrow(new RuntimeException("fail"));

        ResponseEntity<ApiResponse<JornadaDTO>> response = jornadaController.actualizar(1L, dto);

        assertEquals(500, response.getStatusCodeValue());
    }

    @Test
    void eliminar_ok_devuelve200() {
        doNothing().when(jornadaService).eliminar(1L);

        ResponseEntity<ApiResponse<Void>> response = jornadaController.eliminar(1L);

        assertEquals(200, response.getStatusCodeValue());
        assertTrue(response.getBody().isExito());
    }

    @Test
    void eliminar_illegalArgument_devuelve400() {
        doThrow(new IllegalArgumentException("no")).when(jornadaService).eliminar(1L);

        ResponseEntity<ApiResponse<Void>> response = jornadaController.eliminar(1L);

        assertEquals(400, response.getStatusCodeValue());
    }

    @Test
    void eliminar_excepcion_devuelve500() {
        doThrow(new RuntimeException("fail")).when(jornadaService).eliminar(1L);

        ResponseEntity<ApiResponse<Void>> response = jornadaController.eliminar(1L);

        assertEquals(500, response.getStatusCodeValue());
    }

    @Test
    void cambiarEstado_vacio_devuelve400() {
        Map<String, String> request = new HashMap<>();
        request.put("estado", "  ");

        ResponseEntity<ApiResponse<JornadaDTO>> response = jornadaController.cambiarEstado(1L, request);

        assertEquals(400, response.getStatusCodeValue());
        assertTrue(response.getBody().getMensaje().contains("estado es requerido"));
    }

    @Test
    void cambiarEstado_ok_devuelve200() {
        JornadaDTO dto = jornada(1L, "J1");
        doNothing().when(editLockService).assertCanEdit(AuditLog.EntityType.JORNADA, 1L);
        when(jornadaService.cambiarEstado(1L, "cerrada")).thenReturn(dto);
        Map<String, String> request = new HashMap<>();
        request.put("estado", "cerrada");

        ResponseEntity<ApiResponse<JornadaDTO>> response = jornadaController.cambiarEstado(1L, request);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(dto, response.getBody().getDatos());
    }

    @Test
    void cambiarEstado_optimisticLock_devuelve409() {
        when(jornadaService.cambiarEstado(1L, "cerrada"))
                .thenThrow(new ObjectOptimisticLockingFailureException("conflicto", new RuntimeException()));
        Map<String, String> request = new HashMap<>();
        request.put("estado", "cerrada");

        ResponseEntity<ApiResponse<JornadaDTO>> response = jornadaController.cambiarEstado(1L, request);

        assertEquals(409, response.getStatusCodeValue());
    }

    @Test
    void cambiarEstado_illegalArgument_devuelve400() {
        when(jornadaService.cambiarEstado(1L, "x")).thenThrow(new IllegalArgumentException("inválido"));
        Map<String, String> request = new HashMap<>();
        request.put("estado", "x");

        ResponseEntity<ApiResponse<JornadaDTO>> response = jornadaController.cambiarEstado(1L, request);

        assertEquals(400, response.getStatusCodeValue());
    }

    @Test
    void cambiarEstado_excepcion_devuelve500() {
        when(jornadaService.cambiarEstado(1L, "x")).thenThrow(new RuntimeException("fail"));
        Map<String, String> request = new HashMap<>();
        request.put("estado", "x");

        ResponseEntity<ApiResponse<JornadaDTO>> response = jornadaController.cambiarEstado(1L, request);

        assertEquals(500, response.getStatusCodeValue());
    }

    @Test
    void exportarExcel_ok_devuelve200ConNombre() {
        when(jornadaService.exportarExcel(eq(1L), any())).thenReturn(new byte[] {1, 2, 3});
        when(jornadaService.obtenerPorId(1L)).thenReturn(Optional.of(jornada(1L, "Jornada 1")));

        ResponseEntity<byte[]> response = jornadaController.exportarExcel(1L, "ID", "true", "true", "true");

        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertEquals(3, response.getBody().length);
        assertTrue(response.getHeaders().getContentDisposition().toString().contains("jornada_"));
    }

    @Test
    void exportarExcel_jornadaSinFecha_devuelve200() {
        JornadaDTO dto = jornada(1L, "J1");
        dto.setFechaJornada(null);
        when(jornadaService.exportarExcel(eq(1L), any())).thenReturn(new byte[] {1});
        when(jornadaService.obtenerPorId(1L)).thenReturn(Optional.of(dto));

        ResponseEntity<byte[]> response = jornadaController.exportarExcel(1L, null, null, null, null);

        assertEquals(200, response.getStatusCodeValue());
    }

    @Test
    void exportarExcel_jornadaNoEncontrada_usaNombrePorDefecto() {
        when(jornadaService.exportarExcel(eq(1L), any())).thenReturn(new byte[] {1});
        when(jornadaService.obtenerPorId(1L)).thenReturn(Optional.empty());

        ResponseEntity<byte[]> response = jornadaController.exportarExcel(1L, "", "false", "false", "false");

        assertEquals(200, response.getStatusCodeValue());
        assertTrue(response.getHeaders().getContentDisposition().toString().contains("jornada_1"));
    }

    @Test
    void exportarExcel_illegalArgument_devuelve404() {
        when(jornadaService.exportarExcel(eq(9L), any())).thenThrow(new IllegalArgumentException("no"));

        ResponseEntity<byte[]> response = jornadaController.exportarExcel(9L, null, null, null, null);

        assertEquals(404, response.getStatusCodeValue());
        assertNull(response.getBody());
    }

    @Test
    void exportarExcel_excepcion_devuelve500() {
        when(jornadaService.exportarExcel(eq(1L), any())).thenThrow(new RuntimeException("fail"));

        ResponseEntity<byte[]> response = jornadaController.exportarExcel(1L, null, null, null, null);

        assertEquals(500, response.getStatusCodeValue());
    }

    @Test
    void obtenerCuestionariosDisponibles_ok_devuelve200() {
        when(jornadaService.obtenerCuestionariosDisponibles()).thenReturn(Collections.emptyList());

        ResponseEntity<ApiResponse<List<Map<String, Object>>>> response =
                jornadaController.obtenerCuestionariosDisponibles();

        assertEquals(200, response.getStatusCodeValue());
    }

    @Test
    void obtenerCuestionariosDisponibles_excepcion_devuelve500() {
        when(jornadaService.obtenerCuestionariosDisponibles()).thenThrow(new RuntimeException("fail"));

        ResponseEntity<ApiResponse<List<Map<String, Object>>>> response =
                jornadaController.obtenerCuestionariosDisponibles();

        assertEquals(500, response.getStatusCodeValue());
    }

    @Test
    void obtenerCombosDisponibles_ok_devuelve200() {
        when(jornadaService.obtenerCombosDisponibles()).thenReturn(Collections.emptyList());

        ResponseEntity<ApiResponse<List<Map<String, Object>>>> response =
                jornadaController.obtenerCombosDisponibles();

        assertEquals(200, response.getStatusCodeValue());
    }

    @Test
    void obtenerCombosDisponibles_excepcion_devuelve500() {
        when(jornadaService.obtenerCombosDisponibles()).thenThrow(new RuntimeException("fail"));

        ResponseEntity<ApiResponse<List<Map<String, Object>>>> response =
                jornadaController.obtenerCombosDisponibles();

        assertEquals(500, response.getStatusCodeValue());
    }

    @Test
    void reutilizarCuestionario_noAutenticado_devuelve401() {
        when(authService.getCurrentUser()).thenReturn(Optional.empty());

        ResponseEntity<ApiResponse<String>> response = jornadaController.reutilizarCuestionario(1L, 2L);

        assertEquals(401, response.getStatusCodeValue());
    }

    @Test
    void reutilizarCuestionario_ok_devuelve200() {
        when(authService.getCurrentUser()).thenReturn(Optional.of(usuario(1L)));
        doNothing().when(jornadaService).reutilizarCuestionario(1L, 2L, 1L);

        ResponseEntity<ApiResponse<String>> response = jornadaController.reutilizarCuestionario(1L, 2L);

        assertEquals(200, response.getStatusCodeValue());
        assertTrue(response.getBody().getMensaje().contains("reutilizado"));
    }

    @Test
    void reutilizarCuestionario_illegalArgument_devuelve400() {
        when(authService.getCurrentUser()).thenReturn(Optional.of(usuario(1L)));
        doThrow(new IllegalArgumentException("no")).when(jornadaService).reutilizarCuestionario(1L, 2L, 1L);

        ResponseEntity<ApiResponse<String>> response = jornadaController.reutilizarCuestionario(1L, 2L);

        assertEquals(400, response.getStatusCodeValue());
    }

    @Test
    void reutilizarCuestionario_excepcion_devuelve500() {
        when(authService.getCurrentUser()).thenReturn(Optional.of(usuario(1L)));
        doThrow(new RuntimeException("fail")).when(jornadaService).reutilizarCuestionario(1L, 2L, 1L);

        ResponseEntity<ApiResponse<String>> response = jornadaController.reutilizarCuestionario(1L, 2L);

        assertEquals(500, response.getStatusCodeValue());
    }

    @Test
    void quitarReutilizacionCuestionario_ok_devuelve200() {
        doNothing().when(jornadaService).quitarReutilizacionCuestionario(1L, 2L);

        ResponseEntity<ApiResponse<String>> response = jornadaController.quitarReutilizacionCuestionario(1L, 2L);

        assertEquals(200, response.getStatusCodeValue());
    }

    @Test
    void quitarReutilizacionCuestionario_illegalArgument_devuelve400() {
        doThrow(new IllegalArgumentException("no")).when(jornadaService).quitarReutilizacionCuestionario(1L, 2L);

        ResponseEntity<ApiResponse<String>> response = jornadaController.quitarReutilizacionCuestionario(1L, 2L);

        assertEquals(400, response.getStatusCodeValue());
    }

    @Test
    void quitarReutilizacionCuestionario_excepcion_devuelve500() {
        doThrow(new RuntimeException("fail")).when(jornadaService).quitarReutilizacionCuestionario(1L, 2L);

        ResponseEntity<ApiResponse<String>> response = jornadaController.quitarReutilizacionCuestionario(1L, 2L);

        assertEquals(500, response.getStatusCodeValue());
    }

    @Test
    void reutilizarCombo_noAutenticado_devuelve401() {
        when(authService.getCurrentUser()).thenReturn(Optional.empty());

        ResponseEntity<ApiResponse<String>> response = jornadaController.reutilizarCombo(1L, 3L);

        assertEquals(401, response.getStatusCodeValue());
    }

    @Test
    void reutilizarCombo_ok_devuelve200() {
        when(authService.getCurrentUser()).thenReturn(Optional.of(usuario(1L)));
        doNothing().when(jornadaService).reutilizarCombo(1L, 3L, 1L);

        ResponseEntity<ApiResponse<String>> response = jornadaController.reutilizarCombo(1L, 3L);

        assertEquals(200, response.getStatusCodeValue());
    }

    @Test
    void reutilizarCombo_illegalArgument_devuelve400() {
        when(authService.getCurrentUser()).thenReturn(Optional.of(usuario(1L)));
        doThrow(new IllegalArgumentException("no")).when(jornadaService).reutilizarCombo(1L, 3L, 1L);

        ResponseEntity<ApiResponse<String>> response = jornadaController.reutilizarCombo(1L, 3L);

        assertEquals(400, response.getStatusCodeValue());
    }

    @Test
    void reutilizarCombo_excepcion_devuelve500() {
        when(authService.getCurrentUser()).thenReturn(Optional.of(usuario(1L)));
        doThrow(new RuntimeException("fail")).when(jornadaService).reutilizarCombo(1L, 3L, 1L);

        ResponseEntity<ApiResponse<String>> response = jornadaController.reutilizarCombo(1L, 3L);

        assertEquals(500, response.getStatusCodeValue());
    }

    @Test
    void quitarReutilizacionCombo_ok_devuelve200() {
        doNothing().when(jornadaService).quitarReutilizacionCombo(1L, 3L);

        ResponseEntity<ApiResponse<String>> response = jornadaController.quitarReutilizacionCombo(1L, 3L);

        assertEquals(200, response.getStatusCodeValue());
    }

    @Test
    void quitarReutilizacionCombo_illegalArgument_devuelve400() {
        doThrow(new IllegalArgumentException("no")).when(jornadaService).quitarReutilizacionCombo(1L, 3L);

        ResponseEntity<ApiResponse<String>> response = jornadaController.quitarReutilizacionCombo(1L, 3L);

        assertEquals(400, response.getStatusCodeValue());
    }

    @Test
    void quitarReutilizacionCombo_excepcion_devuelve500() {
        doThrow(new RuntimeException("fail")).when(jornadaService).quitarReutilizacionCombo(1L, 3L);

        ResponseEntity<ApiResponse<String>> response = jornadaController.quitarReutilizacionCombo(1L, 3L);

        assertEquals(500, response.getStatusCodeValue());
    }

    @Test
    void reciclarComboEntero_noAutenticado_devuelve401() {
        when(authService.getCurrentUser()).thenReturn(Optional.empty());

        ResponseEntity<ApiResponse<String>> response = jornadaController.reciclarComboEntero(1L, 3L);

        assertEquals(401, response.getStatusCodeValue());
    }

    @Test
    void reciclarComboEntero_ok_devuelve200() {
        when(authService.getCurrentUser()).thenReturn(Optional.of(usuario(1L)));
        doNothing().when(jornadaService).reciclarComboEntero(1L, 3L, 1L);

        ResponseEntity<ApiResponse<String>> response = jornadaController.reciclarComboEntero(1L, 3L);

        assertEquals(200, response.getStatusCodeValue());
    }

    @Test
    void reciclarComboEntero_illegalArgument_devuelve400() {
        when(authService.getCurrentUser()).thenReturn(Optional.of(usuario(1L)));
        doThrow(new IllegalArgumentException("no")).when(jornadaService).reciclarComboEntero(1L, 3L, 1L);

        ResponseEntity<ApiResponse<String>> response = jornadaController.reciclarComboEntero(1L, 3L);

        assertEquals(400, response.getStatusCodeValue());
    }

    @Test
    void reciclarComboEntero_excepcion_devuelve500() {
        when(authService.getCurrentUser()).thenReturn(Optional.of(usuario(1L)));
        doThrow(new RuntimeException("fail")).when(jornadaService).reciclarComboEntero(1L, 3L, 1L);

        ResponseEntity<ApiResponse<String>> response = jornadaController.reciclarComboEntero(1L, 3L);

        assertEquals(500, response.getStatusCodeValue());
    }

    @Test
    void reciclarComboParcial_noAutenticado_devuelve401() {
        when(authService.getCurrentUser()).thenReturn(Optional.empty());

        ResponseEntity<ApiResponse<ReciclajeComboDTO>> response =
                jornadaController.reciclarComboParcial(1L, 3L, new HashMap<>());

        assertEquals(401, response.getStatusCodeValue());
    }

    @Test
    void reciclarComboParcial_sinPregunta_devuelve400() {
        when(authService.getCurrentUser()).thenReturn(Optional.of(usuario(1L)));

        ResponseEntity<ApiResponse<ReciclajeComboDTO>> response =
                jornadaController.reciclarComboParcial(1L, 3L, new HashMap<>());

        assertEquals(400, response.getStatusCodeValue());
        assertTrue(response.getBody().getMensaje().contains("pregunta usada"));
    }

    @Test
    void reciclarComboParcial_okConNumber_devuelve200() {
        when(authService.getCurrentUser()).thenReturn(Optional.of(usuario(1L)));
        ReciclajeComboDTO reciclaje = new ReciclajeComboDTO(1L, 3L, 9L, 5L);
        when(jornadaService.reciclarComboParcial(1L, 3L, 5L, 1L)).thenReturn(reciclaje);
        Map<String, Object> request = new HashMap<>();
        request.put("preguntaUsadaId", 5);

        ResponseEntity<ApiResponse<ReciclajeComboDTO>> response =
                jornadaController.reciclarComboParcial(1L, 3L, request);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(reciclaje, response.getBody().getDatos());
    }

    @Test
    void reciclarComboParcial_okConString_devuelve200() {
        when(authService.getCurrentUser()).thenReturn(Optional.of(usuario(1L)));
        ReciclajeComboDTO reciclaje = new ReciclajeComboDTO(1L, 3L, 9L, 8L);
        when(jornadaService.reciclarComboParcial(1L, 3L, 8L, 1L)).thenReturn(reciclaje);
        Map<String, Object> request = new HashMap<>();
        request.put("preguntaUsadaId", "8");

        ResponseEntity<ApiResponse<ReciclajeComboDTO>> response =
                jornadaController.reciclarComboParcial(1L, 3L, request);

        assertEquals(200, response.getStatusCodeValue());
    }

    @Test
    void reciclarComboParcial_illegalArgument_devuelve400() {
        when(authService.getCurrentUser()).thenReturn(Optional.of(usuario(1L)));
        when(jornadaService.reciclarComboParcial(1L, 3L, 5L, 1L)).thenThrow(new IllegalArgumentException("no"));
        Map<String, Object> request = new HashMap<>();
        request.put("preguntaUsadaId", 5L);

        ResponseEntity<ApiResponse<ReciclajeComboDTO>> response =
                jornadaController.reciclarComboParcial(1L, 3L, request);

        assertEquals(400, response.getStatusCodeValue());
    }

    @Test
    void reciclarComboParcial_excepcion_devuelve500() {
        when(authService.getCurrentUser()).thenReturn(Optional.of(usuario(1L)));
        when(jornadaService.reciclarComboParcial(1L, 3L, 5L, 1L)).thenThrow(new RuntimeException("fail"));
        Map<String, Object> request = new HashMap<>();
        request.put("preguntaUsadaId", 5L);

        ResponseEntity<ApiResponse<ReciclajeComboDTO>> response =
                jornadaController.reciclarComboParcial(1L, 3L, request);

        assertEquals(500, response.getStatusCodeValue());
    }

    @Test
    void cancelarReciclajeCombo_ok_devuelve200() {
        doNothing().when(jornadaService).cancelarReciclajeCombo(1L, 9L);

        ResponseEntity<ApiResponse<String>> response = jornadaController.cancelarReciclajeCombo(1L, 9L);

        assertEquals(200, response.getStatusCodeValue());
    }

    @Test
    void cancelarReciclajeCombo_illegalState_devuelve400() {
        doThrow(new IllegalStateException("ya usado")).when(jornadaService).cancelarReciclajeCombo(1L, 9L);

        ResponseEntity<ApiResponse<String>> response = jornadaController.cancelarReciclajeCombo(1L, 9L);

        assertEquals(400, response.getStatusCodeValue());
        assertEquals("ya usado", response.getBody().getMensaje());
    }

    @Test
    void cancelarReciclajeCombo_excepcion_devuelve500() {
        doThrow(new RuntimeException("fail")).when(jornadaService).cancelarReciclajeCombo(1L, 9L);

        ResponseEntity<ApiResponse<String>> response = jornadaController.cancelarReciclajeCombo(1L, 9L);

        assertEquals(500, response.getStatusCodeValue());
    }
}
