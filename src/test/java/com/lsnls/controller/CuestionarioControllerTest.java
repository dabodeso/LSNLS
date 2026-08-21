package com.lsnls.controller;

import com.lsnls.dto.CrearCuestionarioDTO;
import com.lsnls.entity.AuditLog;
import com.lsnls.entity.Cuestionario;
import com.lsnls.entity.Pregunta;
import com.lsnls.entity.PreguntaCuestionario;
import com.lsnls.entity.Usuario;
import com.lsnls.service.AuthorizationService;
import com.lsnls.service.CuestionarioService;
import com.lsnls.service.EditLockService;
import com.lsnls.service.TematicaService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
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
class CuestionarioControllerTest {

    @Mock
    private CuestionarioService cuestionarioService;
    @Mock
    private TematicaService tematicaService;
    @Mock
    private AuthorizationService authorizationService;
    @Mock
    private EditLockService editLockService;

    @InjectMocks
    private CuestionarioController cuestionarioController;

    private Usuario usuario(Usuario.RolUsuario rol) {
        Usuario u = new Usuario();
        u.setId(1L);
        u.setNombre("user");
        u.setRol(rol);
        return u;
    }

    private Cuestionario cuestionario(Long id, Cuestionario.EstadoCuestionario estado) {
        Cuestionario c = new Cuestionario();
        c.setId(id);
        c.setEstado(estado);
        c.setNivel(Cuestionario.NivelCuestionario.NORMAL);
        c.setTematica("HISTORIA");
        c.setFechaCreacion(LocalDateTime.now());
        c.setPreguntas(new HashSet<PreguntaCuestionario>());
        return c;
    }

    @Test
    void obtenerTodos_ok_devuelve200() {
        Map<String, Object> page = Collections.singletonMap("total", 1);
        when(cuestionarioService.obtenerTodosPaginados(0, 25)).thenReturn(page);

        ResponseEntity<Map<String, Object>> response = cuestionarioController.obtenerTodos(0, 25);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(page, response.getBody());
    }

    @Test
    void obtenerTodos_excepcion_devuelve500() {
        when(cuestionarioService.obtenerTodosPaginados(0, 25)).thenThrow(new RuntimeException("fail"));

        ResponseEntity<Map<String, Object>> response = cuestionarioController.obtenerTodos(0, 25);

        assertEquals(500, response.getStatusCodeValue());
        assertNull(response.getBody());
    }

    @Test
    void obtenerPorId_ok_devuelve200() {
        Cuestionario c = cuestionario(1L, Cuestionario.EstadoCuestionario.borrador);
        when(cuestionarioService.obtenerConPreguntas(1L)).thenReturn(Optional.of(c));

        ResponseEntity<Cuestionario> response = cuestionarioController.obtenerPorId(1L);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(c, response.getBody());
    }

    @Test
    void obtenerPorId_noEncontrado_devuelve404() {
        when(cuestionarioService.obtenerConPreguntas(9L)).thenReturn(Optional.empty());

        ResponseEntity<Cuestionario> response = cuestionarioController.obtenerPorId(9L);

        assertEquals(404, response.getStatusCodeValue());
    }

    @Test
    void obtenerPorId_excepcion_devuelve500() {
        when(cuestionarioService.obtenerConPreguntas(1L)).thenThrow(new RuntimeException("fail"));

        ResponseEntity<Cuestionario> response = cuestionarioController.obtenerPorId(1L);

        assertEquals(500, response.getStatusCodeValue());
    }

    @Test
    void crear_noAutenticado_devuelve401() {
        when(authorizationService.getCurrentUser()).thenReturn(Optional.empty());

        ResponseEntity<?> response = cuestionarioController.crear(cuestionario(null, null));

        assertEquals(401, response.getStatusCodeValue());
        assertEquals("Usuario no autenticado", response.getBody());
    }

    @Test
    void crear_ok_devuelve200() {
        Cuestionario payload = new Cuestionario();
        Cuestionario creado = cuestionario(5L, Cuestionario.EstadoCuestionario.borrador);
        when(authorizationService.getCurrentUser()).thenReturn(Optional.of(usuario(Usuario.RolUsuario.ROLE_GUION)));
        when(cuestionarioService.crear(payload)).thenReturn(creado);

        ResponseEntity<?> response = cuestionarioController.crear(payload);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(creado, response.getBody());
    }

    @Test
    void crear_errorServicio_devuelve400() {
        Cuestionario payload = new Cuestionario();
        when(authorizationService.getCurrentUser()).thenReturn(Optional.of(usuario(Usuario.RolUsuario.ROLE_GUION)));
        when(cuestionarioService.crear(payload)).thenThrow(new RuntimeException("fail"));

        ResponseEntity<?> response = cuestionarioController.crear(payload);

        assertEquals(400, response.getStatusCodeValue());
        assertTrue(response.getBody().toString().contains("Error al crear cuestionario"));
    }

    @Test
    void actualizarNotasDireccion_ok_devuelve200() {
        doNothing().when(editLockService).assertCanEdit(AuditLog.EntityType.CUESTIONARIO, 1L);
        when(cuestionarioService.actualizarNotasDireccion(1L, "notas")).thenReturn(cuestionario(1L, Cuestionario.EstadoCuestionario.borrador));
        Map<String, String> datos = new HashMap<>();
        datos.put("notasDireccion", "notas");

        ResponseEntity<?> response = cuestionarioController.actualizarNotasDireccion(1L, datos);

        assertEquals(200, response.getStatusCodeValue());
    }

    @Test
    void actualizarNotasDireccion_optimisticLock_devuelve409() {
        doThrow(new ObjectOptimisticLockingFailureException("conflicto", new RuntimeException()))
                .when(editLockService).assertCanEdit(AuditLog.EntityType.CUESTIONARIO, 1L);

        ResponseEntity<?> response = cuestionarioController.actualizarNotasDireccion(1L, new HashMap<>());

        assertEquals(409, response.getStatusCodeValue());
    }

    @Test
    void actualizarNotasDireccion_excepcion_devuelve400() {
        doThrow(new RuntimeException("fail")).when(editLockService).assertCanEdit(AuditLog.EntityType.CUESTIONARIO, 1L);

        ResponseEntity<?> response = cuestionarioController.actualizarNotasDireccion(1L, new HashMap<>());

        assertEquals(400, response.getStatusCodeValue());
    }

    @Test
    void filtrarCuestionarios_porId_devuelve200() {
        Map<String, Object> page = Collections.singletonMap("total", 1);
        when(cuestionarioService.filtrarCuestionariosPorId("8", 0, 25)).thenReturn(page);

        ResponseEntity<Map<String, Object>> response =
                cuestionarioController.filtrarCuestionarios(null, null, null, "8", null, 0, 25);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(page, response.getBody());
    }

    @Test
    void filtrarCuestionarios_porFiltros_devuelve200() {
        Map<String, Object> page = Collections.singletonMap("total", 0);
        when(cuestionarioService.filtrarCuestionarios("borrador", "HISTORIA", null, "texto", 0, 25)).thenReturn(page);

        ResponseEntity<Map<String, Object>> response =
                cuestionarioController.filtrarCuestionarios("borrador", "HISTORIA", null, null, "texto", 0, 25);

        assertEquals(200, response.getStatusCodeValue());
    }

    @Test
    void filtrarCuestionarios_excepcion_devuelve500() {
        when(cuestionarioService.filtrarCuestionarios(null, null, null, null, 0, 25))
                .thenThrow(new RuntimeException("fail"));

        ResponseEntity<Map<String, Object>> response =
                cuestionarioController.filtrarCuestionarios(null, null, null, "", null, 0, 25);

        assertEquals(500, response.getStatusCodeValue());
    }

    @Test
    void actualizar_noEncontrado_devuelve404() {
        when(cuestionarioService.obtenerPorId(9L)).thenReturn(Optional.empty());

        ResponseEntity<?> response = cuestionarioController.actualizar(9L, new CrearCuestionarioDTO());

        assertEquals(404, response.getStatusCodeValue());
    }

    @Test
    void actualizar_sinPreguntas_devuelve400() {
        when(cuestionarioService.obtenerPorId(1L))
                .thenReturn(Optional.of(cuestionario(1L, Cuestionario.EstadoCuestionario.borrador)));
        CrearCuestionarioDTO dto = new CrearCuestionarioDTO();
        dto.setPreguntasNormales(Collections.emptyList());

        ResponseEntity<?> response = cuestionarioController.actualizar(1L, dto);

        assertEquals(400, response.getStatusCodeValue());
        assertTrue(response.getBody().toString().contains("al menos una pregunta"));
    }

    @Test
    void actualizar_sinPermiso_devuelve403() {
        when(cuestionarioService.obtenerPorId(1L))
                .thenReturn(Optional.of(cuestionario(1L, Cuestionario.EstadoCuestionario.aprobado)));
        when(authorizationService.canEditCuestionario(Cuestionario.EstadoCuestionario.aprobado)).thenReturn(false);
        CrearCuestionarioDTO dto = new CrearCuestionarioDTO();
        dto.setPreguntasNormales(Collections.singletonList(1L));

        ResponseEntity<?> response = cuestionarioController.actualizar(1L, dto);

        assertEquals(403, response.getStatusCodeValue());
    }

    @Test
    void actualizar_ok_devuelve200() {
        Cuestionario actual = cuestionario(1L, Cuestionario.EstadoCuestionario.borrador);
        Cuestionario actualizado = cuestionario(1L, Cuestionario.EstadoCuestionario.borrador);
        when(cuestionarioService.obtenerPorId(1L)).thenReturn(Optional.of(actual));
        when(authorizationService.canEditCuestionario(Cuestionario.EstadoCuestionario.borrador)).thenReturn(true);
        CrearCuestionarioDTO dto = new CrearCuestionarioDTO();
        dto.setPreguntasNormales(Collections.singletonList(10L));
        when(cuestionarioService.actualizarDesdeDTO(1L, dto)).thenReturn(actualizado);

        ResponseEntity<?> response = cuestionarioController.actualizar(1L, dto);

        assertEquals(200, response.getStatusCodeValue());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals(1L, body.get("id"));
    }

    @Test
    void actualizar_servicioDevuelveNull_devuelve404() {
        when(cuestionarioService.obtenerPorId(1L))
                .thenReturn(Optional.of(cuestionario(1L, Cuestionario.EstadoCuestionario.borrador)));
        when(authorizationService.canEditCuestionario(Cuestionario.EstadoCuestionario.borrador)).thenReturn(true);
        CrearCuestionarioDTO dto = new CrearCuestionarioDTO();
        dto.setPreguntasNormales(Collections.singletonList(10L));
        when(cuestionarioService.actualizarDesdeDTO(1L, dto)).thenReturn(null);

        ResponseEntity<?> response = cuestionarioController.actualizar(1L, dto);

        assertEquals(404, response.getStatusCodeValue());
    }

    @Test
    void actualizar_illegalArgument_devuelve400() {
        when(cuestionarioService.obtenerPorId(1L))
                .thenReturn(Optional.of(cuestionario(1L, Cuestionario.EstadoCuestionario.borrador)));
        when(authorizationService.canEditCuestionario(Cuestionario.EstadoCuestionario.borrador)).thenReturn(true);
        CrearCuestionarioDTO dto = new CrearCuestionarioDTO();
        dto.setPreguntasNormales(Collections.singletonList(10L));
        when(cuestionarioService.actualizarDesdeDTO(1L, dto)).thenThrow(new IllegalArgumentException("inválido"));

        ResponseEntity<?> response = cuestionarioController.actualizar(1L, dto);

        assertEquals(400, response.getStatusCodeValue());
        assertTrue(response.getBody().toString().contains("Error de validación"));
    }

    @Test
    void actualizar_optimisticLock_devuelve409() {
        when(cuestionarioService.obtenerPorId(1L))
                .thenThrow(new ObjectOptimisticLockingFailureException("conflicto", new RuntimeException()));

        ResponseEntity<?> response = cuestionarioController.actualizar(1L, new CrearCuestionarioDTO());

        assertEquals(409, response.getStatusCodeValue());
    }

    @Test
    void cambiarEstado_noEncontrado_devuelve404() {
        when(cuestionarioService.obtenerPorId(9L)).thenReturn(Optional.empty());

        ResponseEntity<?> response = cuestionarioController.cambiarEstado(9L, Cuestionario.EstadoCuestionario.aprobado);

        assertEquals(404, response.getStatusCodeValue());
    }

    @Test
    void cambiarEstado_asignadoAJornada_devuelve409() {
        when(cuestionarioService.obtenerPorId(1L))
                .thenReturn(Optional.of(cuestionario(1L, Cuestionario.EstadoCuestionario.adjudicado)));
        when(cuestionarioService.estaAsignadoAJornada(1L)).thenReturn(true);

        ResponseEntity<?> response = cuestionarioController.cambiarEstado(1L, Cuestionario.EstadoCuestionario.aprobado);

        assertEquals(409, response.getStatusCodeValue());
    }

    @Test
    void cambiarEstado_sinPermiso_devuelve403() {
        when(cuestionarioService.obtenerPorId(1L))
                .thenReturn(Optional.of(cuestionario(1L, Cuestionario.EstadoCuestionario.aprobado)));
        when(cuestionarioService.estaAsignadoAJornada(1L)).thenReturn(false);
        when(authorizationService.canEditCuestionario(Cuestionario.EstadoCuestionario.aprobado)).thenReturn(false);

        ResponseEntity<?> response = cuestionarioController.cambiarEstado(1L, Cuestionario.EstadoCuestionario.borrador);

        assertEquals(403, response.getStatusCodeValue());
    }

    @Test
    void cambiarEstado_ok_devuelve200() {
        Cuestionario c = cuestionario(1L, Cuestionario.EstadoCuestionario.borrador);
        Cuestionario actualizado = cuestionario(1L, Cuestionario.EstadoCuestionario.aprobado);
        when(cuestionarioService.obtenerPorId(1L)).thenReturn(Optional.of(c));
        when(cuestionarioService.estaAsignadoAJornada(1L)).thenReturn(false);
        when(authorizationService.canEditCuestionario(Cuestionario.EstadoCuestionario.borrador)).thenReturn(true);
        when(authorizationService.isAdmin()).thenReturn(true);
        when(cuestionarioService.cambiarEstado(1L, Cuestionario.EstadoCuestionario.aprobado, true)).thenReturn(actualizado);

        ResponseEntity<?> response = cuestionarioController.cambiarEstado(1L, Cuestionario.EstadoCuestionario.aprobado);

        assertEquals(200, response.getStatusCodeValue());
    }

    @Test
    void cambiarEstado_servicioDevuelveNull_devuelve404() {
        Cuestionario c = cuestionario(1L, Cuestionario.EstadoCuestionario.borrador);
        when(cuestionarioService.obtenerPorId(1L)).thenReturn(Optional.of(c));
        when(cuestionarioService.estaAsignadoAJornada(1L)).thenReturn(false);
        when(authorizationService.canEditCuestionario(Cuestionario.EstadoCuestionario.borrador)).thenReturn(true);
        when(authorizationService.isAdmin()).thenReturn(false);
        when(cuestionarioService.cambiarEstado(eq(1L), eq(Cuestionario.EstadoCuestionario.revisar), anyBoolean()))
                .thenReturn(null);

        ResponseEntity<?> response = cuestionarioController.cambiarEstado(1L, Cuestionario.EstadoCuestionario.revisar);

        assertEquals(404, response.getStatusCodeValue());
    }

    @Test
    void cambiarEstado_illegalArgument_devuelve400() {
        Cuestionario c = cuestionario(1L, Cuestionario.EstadoCuestionario.borrador);
        when(cuestionarioService.obtenerPorId(1L)).thenReturn(Optional.of(c));
        when(cuestionarioService.estaAsignadoAJornada(1L)).thenReturn(false);
        when(authorizationService.canEditCuestionario(Cuestionario.EstadoCuestionario.borrador)).thenReturn(true);
        when(authorizationService.isAdmin()).thenReturn(false);
        when(cuestionarioService.cambiarEstado(eq(1L), eq(Cuestionario.EstadoCuestionario.aprobado), anyBoolean()))
                .thenThrow(new IllegalArgumentException("transición inválida"));

        ResponseEntity<?> response = cuestionarioController.cambiarEstado(1L, Cuestionario.EstadoCuestionario.aprobado);

        assertEquals(400, response.getStatusCodeValue());
        assertEquals("transición inválida", response.getBody());
    }

    @Test
    void cambiarTematica_noEncontrado_devuelve404() {
        when(cuestionarioService.obtenerPorId(9L)).thenReturn(Optional.empty());

        ResponseEntity<?> response = cuestionarioController.cambiarTematica(9L, Collections.singletonMap("tematica", "X"));

        assertEquals(404, response.getStatusCodeValue());
    }

    @Test
    void cambiarTematica_sinPermiso_devuelve403() {
        when(cuestionarioService.obtenerPorId(1L))
                .thenReturn(Optional.of(cuestionario(1L, Cuestionario.EstadoCuestionario.grabado)));
        when(authorizationService.canEditCuestionario(Cuestionario.EstadoCuestionario.grabado)).thenReturn(false);

        ResponseEntity<?> response = cuestionarioController.cambiarTematica(1L, Collections.singletonMap("tematica", "X"));

        assertEquals(403, response.getStatusCodeValue());
    }

    @Test
    void cambiarTematica_ok_devuelve200() {
        Cuestionario c = cuestionario(1L, Cuestionario.EstadoCuestionario.borrador);
        Cuestionario actualizado = cuestionario(1L, Cuestionario.EstadoCuestionario.borrador);
        actualizado.setTematica("CIENCIA");
        when(cuestionarioService.obtenerPorId(1L)).thenReturn(Optional.of(c));
        when(authorizationService.canEditCuestionario(Cuestionario.EstadoCuestionario.borrador)).thenReturn(true);
        when(cuestionarioService.cambiarTematica(1L, "CIENCIA")).thenReturn(actualizado);

        ResponseEntity<?> response = cuestionarioController.cambiarTematica(1L, Collections.singletonMap("tematica", "CIENCIA"));

        assertEquals(200, response.getStatusCodeValue());
    }

    @Test
    void cambiarTematica_excepcion_devuelve400() {
        when(cuestionarioService.obtenerPorId(1L)).thenThrow(new RuntimeException("fail"));

        ResponseEntity<?> response = cuestionarioController.cambiarTematica(1L, Collections.singletonMap("tematica", "X"));

        assertEquals(400, response.getStatusCodeValue());
    }

    @Test
    void agregarPregunta_sinPermiso_devuelve403() {
        when(authorizationService.canCreateCuestionario()).thenReturn(false);

        ResponseEntity<?> response = cuestionarioController.agregarPregunta(1L, Collections.singletonMap("preguntaId", 2));

        assertEquals(403, response.getStatusCodeValue());
    }

    @Test
    void agregarPregunta_asignadoAJornada_devuelve409() {
        when(authorizationService.canCreateCuestionario()).thenReturn(true);
        when(cuestionarioService.estaAsignadoAJornada(1L)).thenReturn(true);

        ResponseEntity<?> response = cuestionarioController.agregarPregunta(1L, Collections.singletonMap("preguntaId", 2));

        assertEquals(409, response.getStatusCodeValue());
    }

    @Test
    void agregarPregunta_ok_devuelve200() {
        when(authorizationService.canCreateCuestionario()).thenReturn(true);
        when(cuestionarioService.estaAsignadoAJornada(1L)).thenReturn(false);
        when(cuestionarioService.agregarPregunta(1L, 2L, 1)).thenReturn(true);

        ResponseEntity<?> response = cuestionarioController.agregarPregunta(1L, Collections.singletonMap("preguntaId", 2));

        assertEquals(200, response.getStatusCodeValue());
    }

    @Test
    void agregarPregunta_fallo_devuelve400() {
        when(authorizationService.canCreateCuestionario()).thenReturn(true);
        when(cuestionarioService.estaAsignadoAJornada(1L)).thenReturn(false);
        when(cuestionarioService.agregarPregunta(1L, 2L, 2)).thenReturn(false);
        Map<String, Object> request = new HashMap<>();
        request.put("preguntaId", 2);
        request.put("factorMultiplicacion", 2);

        ResponseEntity<?> response = cuestionarioController.agregarPregunta(1L, request);

        assertEquals(400, response.getStatusCodeValue());
    }

    @Test
    void agregarPregunta_runtime_devuelve400() {
        when(authorizationService.canCreateCuestionario()).thenReturn(true);
        when(cuestionarioService.estaAsignadoAJornada(1L)).thenReturn(false);
        when(cuestionarioService.agregarPregunta(1L, 2L, 1)).thenThrow(new RuntimeException("fail"));

        ResponseEntity<?> response = cuestionarioController.agregarPregunta(1L, Collections.singletonMap("preguntaId", 2));

        assertEquals(400, response.getStatusCodeValue());
    }

    @Test
    void obtenerCuestionariosParaAsignar_ok_devuelve200() {
        when(cuestionarioService.obtenerDisponiblesParaConcursantes())
                .thenReturn(Collections.singletonList(cuestionario(1L, Cuestionario.EstadoCuestionario.aprobado)));

        ResponseEntity<List<Map<String, Object>>> response = cuestionarioController.obtenerCuestionariosParaAsignar();

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(1, response.getBody().size());
        assertEquals(1L, response.getBody().get(0).get("id"));
    }

    @Test
    void obtenerCuestionariosParaAsignar_excepcion_devuelve500() {
        when(cuestionarioService.obtenerDisponiblesParaConcursantes()).thenThrow(new RuntimeException("fail"));

        ResponseEntity<List<Map<String, Object>>> response = cuestionarioController.obtenerCuestionariosParaAsignar();

        assertEquals(500, response.getStatusCodeValue());
    }

    @Test
    void obtenerPorEstado_ok_devuelve200() {
        when(cuestionarioService.obtenerPorEstado(Cuestionario.EstadoCuestionario.borrador))
                .thenReturn(Collections.emptyList());

        ResponseEntity<List<Cuestionario>> response =
                cuestionarioController.obtenerPorEstado(Cuestionario.EstadoCuestionario.borrador);

        assertEquals(200, response.getStatusCodeValue());
    }

    @Test
    void obtenerPorEstado_excepcion_devuelve500() {
        when(cuestionarioService.obtenerPorEstado(Cuestionario.EstadoCuestionario.borrador))
                .thenThrow(new RuntimeException("fail"));

        ResponseEntity<List<Cuestionario>> response =
                cuestionarioController.obtenerPorEstado(Cuestionario.EstadoCuestionario.borrador);

        assertEquals(500, response.getStatusCodeValue());
    }

    @Test
    void obtenerPorNivel_ok_devuelve200() {
        when(cuestionarioService.obtenerPorNivel(Cuestionario.NivelCuestionario.NORMAL))
                .thenReturn(Collections.emptyList());

        ResponseEntity<List<Cuestionario>> response =
                cuestionarioController.obtenerPorNivel(Cuestionario.NivelCuestionario.NORMAL);

        assertEquals(200, response.getStatusCodeValue());
    }

    @Test
    void obtenerPorNivel_excepcion_devuelve500() {
        when(cuestionarioService.obtenerPorNivel(Cuestionario.NivelCuestionario.NORMAL))
                .thenThrow(new RuntimeException("fail"));

        ResponseEntity<List<Cuestionario>> response =
                cuestionarioController.obtenerPorNivel(Cuestionario.NivelCuestionario.NORMAL);

        assertEquals(500, response.getStatusCodeValue());
    }

    @Test
    void eliminar_sinPermiso_devuelve403() {
        when(authorizationService.canDelete()).thenReturn(false);

        ResponseEntity<?> response = cuestionarioController.eliminar(1L);

        assertEquals(403, response.getStatusCodeValue());
    }

    @Test
    void eliminar_ok_devuelve200() {
        when(authorizationService.canDelete()).thenReturn(true);
        when(authorizationService.getCurrentUser()).thenReturn(Optional.of(usuario(Usuario.RolUsuario.ROLE_ADMIN)));
        doNothing().when(cuestionarioService).eliminar(1L);

        ResponseEntity<?> response = cuestionarioController.eliminar(1L);

        assertEquals(200, response.getStatusCodeValue());
    }

    @Test
    void eliminar_illegalArgument_devuelve400() {
        when(authorizationService.canDelete()).thenReturn(true);
        doThrow(new IllegalArgumentException("no")).when(cuestionarioService).eliminar(1L);

        ResponseEntity<?> response = cuestionarioController.eliminar(1L);

        assertEquals(400, response.getStatusCodeValue());
        assertEquals("no", response.getBody());
    }

    @Test
    void eliminar_foreignKey_devuelve400() {
        when(authorizationService.canDelete()).thenReturn(true);
        doThrow(new RuntimeException("constraint fails")).when(cuestionarioService).eliminar(1L);

        ResponseEntity<?> response = cuestionarioController.eliminar(1L);

        assertEquals(400, response.getStatusCodeValue());
        assertTrue(response.getBody().toString().contains("concursantes"));
    }

    @Test
    void eliminar_mensajeJornada_devuelve400() {
        when(authorizationService.canDelete()).thenReturn(true);
        doThrow(new RuntimeException("asignado a jornada")).when(cuestionarioService).eliminar(1L);

        ResponseEntity<?> response = cuestionarioController.eliminar(1L);

        assertEquals(400, response.getStatusCodeValue());
        assertTrue(response.getBody().toString().contains("jornada"));
    }

    @Test
    void eliminar_mensajeConcursante_devuelve400() {
        when(authorizationService.canDelete()).thenReturn(true);
        doThrow(new RuntimeException("asignado a concursante")).when(cuestionarioService).eliminar(1L);

        ResponseEntity<?> response = cuestionarioController.eliminar(1L);

        assertEquals(400, response.getStatusCodeValue());
        assertTrue(response.getBody().toString().contains("concursantes"));
    }

    @Test
    void eliminar_errorGenerico_devuelve400() {
        when(authorizationService.canDelete()).thenReturn(true);
        doThrow(new RuntimeException("otro")).when(cuestionarioService).eliminar(1L);

        ResponseEntity<?> response = cuestionarioController.eliminar(1L);

        assertEquals(400, response.getStatusCodeValue());
        assertTrue(response.getBody().toString().contains("No se pudo eliminar"));
    }

    @Test
    void quitarPregunta_sinPermiso_devuelve403() {
        when(authorizationService.canCreateCuestionario()).thenReturn(false);

        ResponseEntity<?> response = cuestionarioController.quitarPregunta(1L, 2L);

        assertEquals(403, response.getStatusCodeValue());
    }

    @Test
    void quitarPregunta_asignadoAJornada_devuelve409() {
        when(authorizationService.canCreateCuestionario()).thenReturn(true);
        when(cuestionarioService.estaAsignadoAJornada(1L)).thenReturn(true);

        ResponseEntity<?> response = cuestionarioController.quitarPregunta(1L, 2L);

        assertEquals(409, response.getStatusCodeValue());
    }

    @Test
    void quitarPregunta_ok_devuelve200() {
        when(authorizationService.canCreateCuestionario()).thenReturn(true);
        when(cuestionarioService.estaAsignadoAJornada(1L)).thenReturn(false);
        when(cuestionarioService.quitarPregunta(1L, 2L)).thenReturn(true);

        ResponseEntity<?> response = cuestionarioController.quitarPregunta(1L, 2L);

        assertEquals(200, response.getStatusCodeValue());
    }

    @Test
    void quitarPregunta_fallo_devuelve400() {
        when(authorizationService.canCreateCuestionario()).thenReturn(true);
        when(cuestionarioService.estaAsignadoAJornada(1L)).thenReturn(false);
        when(cuestionarioService.quitarPregunta(1L, 2L)).thenReturn(false);

        ResponseEntity<?> response = cuestionarioController.quitarPregunta(1L, 2L);

        assertEquals(400, response.getStatusCodeValue());
    }

    @Test
    void quitarPregunta_runtime_devuelve400() {
        when(authorizationService.canCreateCuestionario()).thenReturn(true);
        when(cuestionarioService.estaAsignadoAJornada(1L)).thenReturn(false);
        when(cuestionarioService.quitarPregunta(1L, 2L)).thenThrow(new RuntimeException("fail"));

        ResponseEntity<?> response = cuestionarioController.quitarPregunta(1L, 2L);

        assertEquals(400, response.getStatusCodeValue());
    }

    @Test
    void quitarPreguntaPorSlot_sinPermiso_devuelve403() {
        when(authorizationService.canCreateCuestionario()).thenReturn(false);

        ResponseEntity<?> response = cuestionarioController.quitarPreguntaPorSlot(1L, "1LS");

        assertEquals(403, response.getStatusCodeValue());
    }

    @Test
    void quitarPreguntaPorSlot_asignadoAJornada_devuelve409() {
        when(authorizationService.canCreateCuestionario()).thenReturn(true);
        when(cuestionarioService.estaAsignadoAJornada(1L)).thenReturn(true);

        ResponseEntity<?> response = cuestionarioController.quitarPreguntaPorSlot(1L, "1LS");

        assertEquals(409, response.getStatusCodeValue());
    }

    @Test
    void quitarPreguntaPorSlot_ok_devuelve200() {
        when(authorizationService.canCreateCuestionario()).thenReturn(true);
        when(cuestionarioService.estaAsignadoAJornada(1L)).thenReturn(false);
        when(cuestionarioService.quitarPreguntaPorSlot(1L, "1LS")).thenReturn(true);

        ResponseEntity<?> response = cuestionarioController.quitarPreguntaPorSlot(1L, "1LS");

        assertEquals(200, response.getStatusCodeValue());
    }

    @Test
    void quitarPreguntaPorSlot_fallo_devuelve400() {
        when(authorizationService.canCreateCuestionario()).thenReturn(true);
        when(cuestionarioService.estaAsignadoAJornada(1L)).thenReturn(false);
        when(cuestionarioService.quitarPreguntaPorSlot(1L, "1LS")).thenReturn(false);

        ResponseEntity<?> response = cuestionarioController.quitarPreguntaPorSlot(1L, "1LS");

        assertEquals(400, response.getStatusCodeValue());
        assertTrue(response.getBody().toString().contains("slot"));
    }

    @Test
    void quitarPreguntaPorSlot_runtime_devuelve400() {
        when(authorizationService.canCreateCuestionario()).thenReturn(true);
        when(cuestionarioService.estaAsignadoAJornada(1L)).thenReturn(false);
        when(cuestionarioService.quitarPreguntaPorSlot(1L, "1LS")).thenThrow(new RuntimeException("fail"));

        ResponseEntity<?> response = cuestionarioController.quitarPreguntaPorSlot(1L, "1LS");

        assertEquals(400, response.getStatusCodeValue());
    }

    @Test
    void debugPermisos_ok_devuelve200() {
        when(authorizationService.getCurrentUser()).thenReturn(Optional.of(usuario(Usuario.RolUsuario.ROLE_DIRECCION)));
        when(authorizationService.canCreateCuestionario()).thenReturn(true);
        when(authorizationService.canRead()).thenReturn(true);
        when(authorizationService.canDelete()).thenReturn(true);

        ResponseEntity<Map<String, Object>> response = cuestionarioController.debugPermisos();

        assertEquals(200, response.getStatusCodeValue());
        assertEquals("user", response.getBody().get("currentUser"));
    }

    @Test
    void debugPermisos_noAutenticado_devuelve401() {
        when(authorizationService.getCurrentUser()).thenReturn(Optional.empty());

        ResponseEntity<Map<String, Object>> response = cuestionarioController.debugPermisos();

        assertEquals(401, response.getStatusCodeValue());
    }

    @Test
    void debugPregunta_ok_devuelve200() {
        Pregunta p = new Pregunta();
        p.setId(1L);
        p.setEstado(Pregunta.EstadoPregunta.borrador);
        p.setEstadoDisponibilidad(Pregunta.EstadoDisponibilidad.disponible);
        p.setPregunta("¿Capital?");
        p.setRespuesta("Madrid ñ");
        p.setNivel(Pregunta.NivelPregunta._1LS);
        p.setCreacionUsuario(usuario(Usuario.RolUsuario.ROLE_GUION));
        when(cuestionarioService.obtenerPreguntaPorId(1L)).thenReturn(Optional.of(p));

        ResponseEntity<Map<String, Object>> response = cuestionarioController.debugPregunta(1L);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(1L, response.getBody().get("id"));
    }

    @Test
    void debugPregunta_noEncontrada_devuelve200ConError() {
        when(cuestionarioService.obtenerPreguntaPorId(9L)).thenReturn(Optional.empty());

        ResponseEntity<Map<String, Object>> response = cuestionarioController.debugPregunta(9L);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals("Pregunta no encontrada", response.getBody().get("error"));
    }

    @Test
    void debugPregunta_excepcion_devuelve200ConError() {
        when(cuestionarioService.obtenerPreguntaPorId(1L)).thenThrow(new RuntimeException("fail"));

        ResponseEntity<Map<String, Object>> response = cuestionarioController.debugPregunta(1L);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals("fail", response.getBody().get("error"));
    }

    @Test
    void debugSimple_ok_devuelve200() {
        Pregunta p = new Pregunta();
        p.setRespuesta("Madrid");
        p.setEstado(Pregunta.EstadoPregunta.borrador);
        p.setEstadoDisponibilidad(Pregunta.EstadoDisponibilidad.disponible);
        when(cuestionarioService.obtenerPreguntaPorId(1L)).thenReturn(Optional.of(p));

        ResponseEntity<String> response = cuestionarioController.debugSimple(1L);

        assertEquals(200, response.getStatusCodeValue());
        assertTrue(response.getBody().contains("Madrid"));
    }

    @Test
    void debugSimple_noEncontrada_devuelve200() {
        when(cuestionarioService.obtenerPreguntaPorId(9L)).thenReturn(Optional.empty());

        ResponseEntity<String> response = cuestionarioController.debugSimple(9L);

        assertEquals(200, response.getStatusCodeValue());
        assertTrue(response.getBody().contains("no encontrada"));
    }

    @Test
    void debugSimple_excepcion_devuelve200() {
        when(cuestionarioService.obtenerPreguntaPorId(1L)).thenThrow(new RuntimeException("fail"));

        ResponseEntity<String> response = cuestionarioController.debugSimple(1L);

        assertEquals(200, response.getStatusCodeValue());
        assertTrue(response.getBody().contains("Error: fail"));
    }

    @Test
    void debugSql_ok_devuelve200() {
        when(cuestionarioService.obtenerPorId(1L))
                .thenReturn(Optional.of(cuestionario(1L, Cuestionario.EstadoCuestionario.borrador)));
        when(cuestionarioService.obtenerPreguntasPorCuestionarioSQL(1L))
                .thenReturn(Collections.singletonList(new Object[] {1L, 1L, 1, "pregunta", "respuesta"}));

        ResponseEntity<Map<String, Object>> response = cuestionarioController.debugSql(1L);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(true, response.getBody().get("cuestionarioExists"));
        assertEquals(1, response.getBody().get("preguntasEncontradas"));
    }

    @Test
    void debugSql_sinCuestionario_devuelve200() {
        when(cuestionarioService.obtenerPorId(9L)).thenReturn(Optional.empty());

        ResponseEntity<Map<String, Object>> response = cuestionarioController.debugSql(9L);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(false, response.getBody().get("cuestionarioExists"));
    }

    @Test
    void debugSql_excepcion_devuelve200ConError() {
        when(cuestionarioService.obtenerPorId(1L)).thenThrow(new RuntimeException("fail"));

        ResponseEntity<Map<String, Object>> response = cuestionarioController.debugSql(1L);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals("fail", response.getBody().get("error"));
    }

    @Test
    void crearDesdeDTO_sinPreguntas_devuelve400() {
        CrearCuestionarioDTO dto = new CrearCuestionarioDTO();
        dto.setPreguntasNormales(Collections.emptyList());

        ResponseEntity<?> response = cuestionarioController.crearDesdeDTO(dto);

        assertEquals(400, response.getStatusCodeValue());
    }

    @Test
    void crearDesdeDTO_sinPermiso_devuelve403() {
        CrearCuestionarioDTO dto = new CrearCuestionarioDTO();
        dto.setPreguntasNormales(Collections.singletonList(1L));
        when(authorizationService.canCreateCuestionario()).thenReturn(false);

        ResponseEntity<?> response = cuestionarioController.crearDesdeDTO(dto);

        assertEquals(403, response.getStatusCodeValue());
    }

    @Test
    void crearDesdeDTO_noAutenticado_devuelve401() {
        CrearCuestionarioDTO dto = new CrearCuestionarioDTO();
        dto.setPreguntasNormales(Collections.singletonList(1L));
        when(authorizationService.canCreateCuestionario()).thenReturn(true);
        when(authorizationService.getCurrentUser()).thenReturn(Optional.empty());

        ResponseEntity<?> response = cuestionarioController.crearDesdeDTO(dto);

        assertEquals(401, response.getStatusCodeValue());
    }

    @Test
    void crearDesdeDTO_ok_devuelve200() {
        CrearCuestionarioDTO dto = new CrearCuestionarioDTO();
        dto.setPreguntasNormales(Collections.singletonList(1L));
        Usuario user = usuario(Usuario.RolUsuario.ROLE_GUION);
        when(authorizationService.canCreateCuestionario()).thenReturn(true);
        when(authorizationService.getCurrentUser()).thenReturn(Optional.of(user));
        when(cuestionarioService.crearDesdeDTO(dto, user)).thenReturn(cuestionario(7L, Cuestionario.EstadoCuestionario.borrador));

        ResponseEntity<?> response = cuestionarioController.crearDesdeDTO(dto);

        assertEquals(200, response.getStatusCodeValue());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals(7L, body.get("id"));
    }

    @Test
    void crearDesdeDTO_illegalArgument_devuelve400() {
        CrearCuestionarioDTO dto = new CrearCuestionarioDTO();
        dto.setPreguntasNormales(Collections.singletonList(1L));
        Usuario user = usuario(Usuario.RolUsuario.ROLE_GUION);
        when(authorizationService.canCreateCuestionario()).thenReturn(true);
        when(authorizationService.getCurrentUser()).thenReturn(Optional.of(user));
        when(cuestionarioService.crearDesdeDTO(dto, user)).thenThrow(new IllegalArgumentException("inválido"));

        ResponseEntity<?> response = cuestionarioController.crearDesdeDTO(dto);

        assertEquals(400, response.getStatusCodeValue());
        assertTrue(response.getBody().toString().contains("Error de validación"));
    }

    @Test
    void crearDesdeDTO_excepcion_devuelve400() {
        CrearCuestionarioDTO dto = new CrearCuestionarioDTO();
        dto.setPreguntasNormales(Collections.singletonList(1L));
        Usuario user = usuario(Usuario.RolUsuario.ROLE_GUION);
        when(authorizationService.canCreateCuestionario()).thenReturn(true);
        when(authorizationService.getCurrentUser()).thenReturn(Optional.of(user));
        when(cuestionarioService.crearDesdeDTO(dto, user)).thenThrow(new RuntimeException("fail"));

        ResponseEntity<?> response = cuestionarioController.crearDesdeDTO(dto);

        assertEquals(400, response.getStatusCodeValue());
        assertTrue(response.getBody().toString().contains("Error interno"));
    }

    @Test
    void obtenerTematicas_ok_devuelve200() {
        when(tematicaService.obtenerNombresTematicas()).thenReturn(Collections.singletonList("HISTORIA"));

        ResponseEntity<List<String>> response = cuestionarioController.obtenerTematicas();

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void obtenerTematicas_excepcion_devuelve500() {
        when(tematicaService.obtenerNombresTematicas()).thenThrow(new RuntimeException("fail"));

        ResponseEntity<List<String>> response = cuestionarioController.obtenerTematicas();

        assertEquals(500, response.getStatusCodeValue());
    }

    @Test
    void añadirTematica_vacia_devuelve400() {
        Map<String, String> datos = new HashMap<>();
        datos.put("tematica", "  ");

        ResponseEntity<?> response = cuestionarioController.añadirTematica(datos);

        assertEquals(400, response.getStatusCodeValue());
    }

    @Test
    void añadirTematica_noAutenticado_devuelve401() {
        when(authorizationService.getCurrentUser()).thenReturn(Optional.empty());
        Map<String, String> datos = new HashMap<>();
        datos.put("tematica", "HISTORIA");

        ResponseEntity<?> response = cuestionarioController.añadirTematica(datos);

        assertEquals(401, response.getStatusCodeValue());
    }

    @Test
    void añadirTematica_ok_devuelve200() {
        Usuario user = usuario(Usuario.RolUsuario.ROLE_GUION);
        when(authorizationService.getCurrentUser()).thenReturn(Optional.of(user));
        when(tematicaService.añadirTematica("HISTORIA", user)).thenReturn(new com.lsnls.entity.Tematica());
        Map<String, String> datos = new HashMap<>();
        datos.put("tematica", " HISTORIA ");

        ResponseEntity<?> response = cuestionarioController.añadirTematica(datos);

        assertEquals(200, response.getStatusCodeValue());
    }

    @Test
    void añadirTematica_excepcion_devuelve400() {
        Usuario user = usuario(Usuario.RolUsuario.ROLE_GUION);
        when(authorizationService.getCurrentUser()).thenReturn(Optional.of(user));
        doThrow(new RuntimeException("duplicada")).when(tematicaService).añadirTematica("X", user);
        Map<String, String> datos = new HashMap<>();
        datos.put("tematica", "X");

        ResponseEntity<?> response = cuestionarioController.añadirTematica(datos);

        assertEquals(400, response.getStatusCodeValue());
    }

    @Test
    void eliminarTematica_ok_devuelve200() {
        when(tematicaService.eliminarTematica("HISTORIA")).thenReturn(true);

        ResponseEntity<?> response = cuestionarioController.eliminarTematica("HISTORIA");

        assertEquals(200, response.getStatusCodeValue());
    }

    @Test
    void eliminarTematica_illegalState_devuelve400() {
        doThrow(new IllegalStateException("en uso")).when(tematicaService).eliminarTematica("HISTORIA");

        ResponseEntity<?> response = cuestionarioController.eliminarTematica("HISTORIA");

        assertEquals(400, response.getStatusCodeValue());
        assertEquals("en uso", response.getBody());
    }

    @Test
    void eliminarTematica_excepcion_devuelve400() {
        doThrow(new RuntimeException("fail")).when(tematicaService).eliminarTematica("HISTORIA");

        ResponseEntity<?> response = cuestionarioController.eliminarTematica("HISTORIA");

        assertEquals(400, response.getStatusCodeValue());
    }

    @Test
    void obtenerEstadisticasTematicas_ok_devuelve200() {
        when(tematicaService.obtenerEstadisticas()).thenReturn(Collections.singletonMap("total", 3));

        ResponseEntity<Map<String, Object>> response = cuestionarioController.obtenerEstadisticasTematicas();

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(3, response.getBody().get("total"));
    }

    @Test
    void obtenerEstadisticasTematicas_excepcion_devuelve500() {
        when(tematicaService.obtenerEstadisticas()).thenThrow(new RuntimeException("fail"));

        ResponseEntity<Map<String, Object>> response = cuestionarioController.obtenerEstadisticasTematicas();

        assertEquals(500, response.getStatusCodeValue());
    }
}
