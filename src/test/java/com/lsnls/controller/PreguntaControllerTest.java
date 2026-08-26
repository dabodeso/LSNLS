package com.lsnls.controller;

import com.lsnls.dto.PreguntaCreateDTO;
import com.lsnls.dto.PreguntaDTO;
import com.lsnls.entity.Pregunta;
import com.lsnls.entity.Usuario;
import com.lsnls.repository.PreguntaRepository;
import com.lsnls.service.AuthorizationService;
import com.lsnls.service.DataTransformationService;
import com.lsnls.service.EditLockService;
import com.lsnls.service.PreguntaService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PreguntaControllerTest {

    @Mock
    private PreguntaService preguntaService;
    @Mock
    private AuthorizationService authService;
    @Mock
    private DataTransformationService dataTransformationService;
    @Mock
    private EditLockService editLockService;
    @Mock
    private PreguntaRepository preguntaRepository;

    @InjectMocks
    private PreguntaController preguntaController;

    private Usuario usuario(Usuario.RolUsuario rol) {
        Usuario u = new Usuario();
        u.setId(1L);
        u.setNombre("user");
        u.setRol(rol);
        return u;
    }

    private Pregunta pregunta(Long id, Pregunta.EstadoPregunta estado) {
        Pregunta p = new Pregunta();
        p.setId(id);
        p.setEstado(estado);
        p.setNivel(Pregunta.NivelPregunta._1LS);
        p.setTematica("HISTORIA");
        p.setPregunta("¿Capital de España?");
        p.setRespuesta("Madrid");
        p.setAutor("user");
        return p;
    }

    private PreguntaDTO dto(Long id) {
        PreguntaDTO dto = new PreguntaDTO();
        dto.setId(id);
        dto.setPregunta("¿Capital?");
        dto.setAutor("user");
        dto.setEstado("borrador");
        return dto;
    }

    private PreguntaCreateDTO createDto() {
        PreguntaCreateDTO dto = new PreguntaCreateDTO();
        dto.nivel = "_1LS";
        dto.tematica = "HISTORIA";
        dto.pregunta = "¿Capital de España?";
        dto.respuesta = "Madrid";
        return dto;
    }

    @Test
    void obtenerPaginadas_okConContenido_devuelve200() {
        Page<PreguntaDTO> page = new PageImpl<>(Collections.singletonList(dto(1L)), PageRequest.of(0, 25), 1);
        when(preguntaService.obtenerPaginadasDTO(any(Pageable.class))).thenReturn(page);

        ResponseEntity<Page<PreguntaDTO>> response = preguntaController.obtenerPaginadas(0, 25, "id", "desc");

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(1, response.getBody().getContent().size());
    }

    @Test
    void obtenerPaginadas_okVacioAsc_devuelve200() {
        Page<PreguntaDTO> page = new PageImpl<>(Collections.emptyList());
        when(preguntaService.obtenerPaginadasDTO(any(Pageable.class))).thenReturn(page);

        ResponseEntity<Page<PreguntaDTO>> response = preguntaController.obtenerPaginadas(0, 25, "id", "asc");

        assertEquals(200, response.getStatusCodeValue());
        assertTrue(response.getBody().getContent().isEmpty());
    }

    @Test
    void obtenerPaginadas_excepcion_devuelve500() {
        when(preguntaService.obtenerPaginadasDTO(any(Pageable.class))).thenThrow(new RuntimeException("fail"));

        ResponseEntity<Page<PreguntaDTO>> response = preguntaController.obtenerPaginadas(0, 25, "id", "desc");

        assertEquals(500, response.getStatusCodeValue());
        assertNull(response.getBody());
    }

    @Test
    void obtenerPaginadasPageable_ok_devuelve200() {
        Page<Pregunta> page = new PageImpl<>(Collections.emptyList());
        when(preguntaService.obtenerPaginadas(any(Pageable.class))).thenReturn(page);

        ResponseEntity<Page<Pregunta>> response = preguntaController.obtenerPaginadas(PageRequest.of(0, 10));

        assertEquals(200, response.getStatusCodeValue());
    }

    @Test
    void obtenerPaginadasPageable_excepcion_devuelve500() {
        when(preguntaService.obtenerPaginadas(any(Pageable.class))).thenThrow(new RuntimeException("fail"));

        ResponseEntity<Page<Pregunta>> response = preguntaController.obtenerPaginadas(PageRequest.of(0, 10));

        assertEquals(500, response.getStatusCodeValue());
    }

    @Test
    void cargarMasPreguntas_ok_devuelve200() {
        Page<PreguntaDTO> page = new PageImpl<>(Collections.singletonList(dto(2L)));
        when(preguntaService.obtenerPaginadasDTO(any(Pageable.class))).thenReturn(page);

        ResponseEntity<Page<PreguntaDTO>> response = preguntaController.cargarMasPreguntas(1, 25);

        assertEquals(200, response.getStatusCodeValue());
    }

    @Test
    void cargarMasPreguntas_excepcion_devuelve500() {
        when(preguntaService.obtenerPaginadasDTO(any(Pageable.class))).thenThrow(new RuntimeException("fail"));

        ResponseEntity<Page<PreguntaDTO>> response = preguntaController.cargarMasPreguntas(0, 25);

        assertEquals(500, response.getStatusCodeValue());
    }

    @Test
    void obtenerPorId_ok_devuelve200() {
        PreguntaDTO dto = dto(1L);
        when(preguntaService.obtenerPorIdDTO(1L)).thenReturn(Optional.of(dto));

        ResponseEntity<PreguntaDTO> response = preguntaController.obtenerPorId(1L);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(dto, response.getBody());
    }

    @Test
    void obtenerPorId_noEncontrada_devuelve404() {
        when(preguntaService.obtenerPorIdDTO(9L)).thenReturn(Optional.empty());

        ResponseEntity<PreguntaDTO> response = preguntaController.obtenerPorId(9L);

        assertEquals(404, response.getStatusCodeValue());
    }

    @Test
    void obtenerPorId_excepcion_devuelve500() {
        when(preguntaService.obtenerPorIdDTO(1L)).thenThrow(new RuntimeException("fail"));

        ResponseEntity<PreguntaDTO> response = preguntaController.obtenerPorId(1L);

        assertEquals(500, response.getStatusCodeValue());
    }

    @Test
    void crear_nivelVacio_devuelve400() {
        PreguntaCreateDTO dto = createDto();
        dto.nivel = " ";

        ResponseEntity<?> response = preguntaController.crear(dto);

        assertEquals(400, response.getStatusCodeValue());
        assertTrue(response.getBody().toString().contains("nivel"));
    }

    @Test
    void crear_tematicaVacia_devuelve400() {
        PreguntaCreateDTO dto = createDto();
        dto.tematica = "";

        ResponseEntity<?> response = preguntaController.crear(dto);

        assertEquals(400, response.getStatusCodeValue());
        assertTrue(response.getBody().toString().contains("temática"));
    }

    @Test
    void crear_preguntaVacia_devuelve400() {
        PreguntaCreateDTO dto = createDto();
        dto.pregunta = " ";

        ResponseEntity<?> response = preguntaController.crear(dto);

        assertEquals(400, response.getStatusCodeValue());
        assertTrue(response.getBody().toString().contains("'pregunta'"));
    }

    @Test
    void crear_respuestaVacia_devuelve400() {
        PreguntaCreateDTO dto = createDto();
        dto.respuesta = "";

        ResponseEntity<?> response = preguntaController.crear(dto);

        assertEquals(400, response.getStatusCodeValue());
        assertTrue(response.getBody().toString().contains("respuesta"));
    }

    @Test
    void crear_nivelInvalido_devuelve400() {
        PreguntaCreateDTO dto = createDto();
        dto.nivel = "NIVEL_X";

        ResponseEntity<?> response = preguntaController.crear(dto);

        assertEquals(400, response.getStatusCodeValue());
        assertTrue(response.getBody().toString().contains("no válido"));
    }

    @Test
    void crear_sinPermiso_devuelve403() {
        when(authService.canCreatePregunta()).thenReturn(false);

        ResponseEntity<?> response = preguntaController.crear(createDto());

        assertEquals(403, response.getStatusCodeValue());
    }

    @Test
    void crear_noAutenticado_devuelve401() {
        when(authService.canCreatePregunta()).thenReturn(true);
        when(authService.getCurrentUser()).thenReturn(Optional.empty());

        ResponseEntity<?> response = preguntaController.crear(createDto());

        assertEquals(401, response.getStatusCodeValue());
    }

    @Test
    void crear_estadoInvalido_devuelve400() {
        PreguntaCreateDTO dto = createDto();
        dto.estado = "inventado";
        when(authService.canCreatePregunta()).thenReturn(true);
        when(authService.getCurrentUser()).thenReturn(Optional.of(usuario(Usuario.RolUsuario.ROLE_GUION)));

        ResponseEntity<?> response = preguntaController.crear(dto);

        assertEquals(400, response.getStatusCodeValue());
        assertTrue(response.getBody().toString().contains("Estado inválido"));
    }

    @Test
    void crear_estadoNoPermitido_devuelve400() {
        PreguntaCreateDTO dto = createDto();
        dto.estado = "aprobada";
        when(authService.canCreatePregunta()).thenReturn(true);
        when(authService.getCurrentUser()).thenReturn(Optional.of(usuario(Usuario.RolUsuario.ROLE_GUION)));

        ResponseEntity<?> response = preguntaController.crear(dto);

        assertEquals(400, response.getStatusCodeValue());
        assertTrue(response.getBody().toString().contains("borrador"));
    }

    @Test
    void crear_paraVerificarSinPermiso_devuelve403() {
        PreguntaCreateDTO dto = createDto();
        dto.estado = "para_verificar";
        dto.fuentes = "fuente";
        when(authService.canCreatePregunta()).thenReturn(true);
        when(authService.getCurrentUser()).thenReturn(Optional.of(usuario(Usuario.RolUsuario.ROLE_GUION)));
        doNothing().when(preguntaService).validarRequisitosParaVerificar(any(), any(), any(), any());
        when(authService.canChangeEstadoPregunta(Pregunta.EstadoPregunta.borrador, Pregunta.EstadoPregunta.para_verificar))
                .thenReturn(false);

        ResponseEntity<?> response = preguntaController.crear(dto);

        assertEquals(403, response.getStatusCodeValue());
    }

    @Test
    void crear_okBorrador_devuelve200() {
        PreguntaCreateDTO dto = createDto();
        Pregunta creada = pregunta(8L, Pregunta.EstadoPregunta.borrador);
        when(authService.canCreatePregunta()).thenReturn(true);
        when(authService.getCurrentUser()).thenReturn(Optional.of(usuario(Usuario.RolUsuario.ROLE_GUION)));
        when(preguntaService.crear(any(Pregunta.class))).thenReturn(creada);

        ResponseEntity<?> response = preguntaController.crear(dto);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(creada, response.getBody());
    }

    @Test
    void crear_paraVerificarOk_devuelve200() {
        PreguntaCreateDTO dto = createDto();
        dto.estado = "para_verificar";
        dto.fuentes = "fuente";
        Pregunta creada = pregunta(8L, Pregunta.EstadoPregunta.para_verificar);
        when(authService.canCreatePregunta()).thenReturn(true);
        when(authService.getCurrentUser()).thenReturn(Optional.of(usuario(Usuario.RolUsuario.ROLE_VERIFICACION)));
        when(authService.canChangeEstadoPregunta(Pregunta.EstadoPregunta.borrador, Pregunta.EstadoPregunta.para_verificar))
                .thenReturn(true);
        when(preguntaService.crear(any(Pregunta.class))).thenReturn(creada);

        ResponseEntity<?> response = preguntaController.crear(dto);

        assertEquals(200, response.getStatusCodeValue());
    }

    @Test
    void crear_illegalArgumentServicio_devuelve400() {
        when(authService.canCreatePregunta()).thenReturn(true);
        when(authService.getCurrentUser()).thenReturn(Optional.of(usuario(Usuario.RolUsuario.ROLE_GUION)));
        when(preguntaService.crear(any(Pregunta.class))).thenThrow(new IllegalArgumentException("inválido"));

        ResponseEntity<?> response = preguntaController.crear(createDto());

        assertEquals(400, response.getStatusCodeValue());
        assertTrue(response.getBody().toString().contains("Error de validación"));
    }

    @Test
    void crear_excepcionServicio_devuelve400() {
        when(authService.canCreatePregunta()).thenReturn(true);
        when(authService.getCurrentUser()).thenReturn(Optional.of(usuario(Usuario.RolUsuario.ROLE_GUION)));
        when(preguntaService.crear(any(Pregunta.class))).thenThrow(new RuntimeException("fail"));

        ResponseEntity<?> response = preguntaController.crear(createDto());

        assertEquals(400, response.getStatusCodeValue());
        assertTrue(response.getBody().toString().contains("Error interno"));
    }

    @Test
    void restaurarEliminada_ok_devuelve200() {
        PreguntaDTO snapshot = dto(1L);
        Pregunta restaurada = pregunta(1L, Pregunta.EstadoPregunta.borrador);
        when(preguntaService.restaurarDesdeSnapshot(snapshot)).thenReturn(restaurada);
        when(preguntaService.mapPreguntaToDTO(restaurada)).thenReturn(snapshot);

        ResponseEntity<?> response = preguntaController.restaurarEliminada(snapshot);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(snapshot, response.getBody());
    }

    @Test
    void restaurarEliminada_illegalArgument_devuelve400() {
        PreguntaDTO snapshot = dto(1L);
        when(preguntaService.restaurarDesdeSnapshot(snapshot)).thenThrow(new IllegalArgumentException("incompleto"));

        ResponseEntity<?> response = preguntaController.restaurarEliminada(snapshot);

        assertEquals(400, response.getStatusCodeValue());
        assertEquals("incompleto", response.getBody());
    }

    @Test
    void restaurarEliminada_excepcion_devuelve400() {
        PreguntaDTO snapshot = dto(1L);
        when(preguntaService.restaurarDesdeSnapshot(snapshot)).thenThrow(new RuntimeException("fail"));

        ResponseEntity<?> response = preguntaController.restaurarEliminada(snapshot);

        assertEquals(400, response.getStatusCodeValue());
        assertTrue(response.getBody().toString().contains("Error al restaurar"));
    }

    @Test
    void actualizar_noEncontrada_devuelve404() {
        when(preguntaService.obtenerPorId(9L)).thenReturn(Optional.empty());

        ResponseEntity<?> response = preguntaController.actualizar(9L, dto(9L));

        assertEquals(404, response.getStatusCodeValue());
    }

    @Test
    void actualizar_sinPermisoEditar_devuelve403() {
        Pregunta p = pregunta(1L, Pregunta.EstadoPregunta.aprobada);
        when(preguntaService.obtenerPorId(1L)).thenReturn(Optional.of(p));
        when(authService.canEditPregunta(Pregunta.EstadoPregunta.aprobada)).thenReturn(false);

        ResponseEntity<?> response = preguntaController.actualizar(1L, dto(1L));

        assertEquals(403, response.getStatusCodeValue());
        assertTrue(response.getBody().toString().contains("aprobada"));
    }

    @Test
    void actualizar_cambioEstadoSinPermiso_devuelve403() {
        Pregunta p = pregunta(1L, Pregunta.EstadoPregunta.borrador);
        PreguntaDTO dto = dto(1L);
        dto.setEstado("verificada");
        when(preguntaService.obtenerPorId(1L)).thenReturn(Optional.of(p));
        when(authService.canEditPregunta(Pregunta.EstadoPregunta.borrador)).thenReturn(true);
        when(authService.canChangeEstadoPregunta(Pregunta.EstadoPregunta.borrador, Pregunta.EstadoPregunta.verificada))
                .thenReturn(false);

        ResponseEntity<?> response = preguntaController.actualizar(1L, dto);

        assertEquals(403, response.getStatusCodeValue());
    }

    @Test
    void actualizar_estadoInvalido_devuelve400() {
        Pregunta p = pregunta(1L, Pregunta.EstadoPregunta.borrador);
        PreguntaDTO dto = dto(1L);
        dto.setEstado("NO_EXISTE");
        when(preguntaService.obtenerPorId(1L)).thenReturn(Optional.of(p));
        when(authService.canEditPregunta(Pregunta.EstadoPregunta.borrador)).thenReturn(true);

        ResponseEntity<?> response = preguntaController.actualizar(1L, dto);

        assertEquals(400, response.getStatusCodeValue());
        assertTrue(response.getBody().toString().contains("Estado inválido"));
    }

    @Test
    void actualizar_ok_devuelve200() {
        Pregunta p = pregunta(1L, Pregunta.EstadoPregunta.borrador);
        PreguntaDTO dto = dto(1L);
        dto.setEstado("borrador");
        Pregunta actualizada = pregunta(1L, Pregunta.EstadoPregunta.borrador);
        when(preguntaService.obtenerPorId(1L)).thenReturn(Optional.of(p));
        when(authService.canEditPregunta(Pregunta.EstadoPregunta.borrador)).thenReturn(true);
        when(preguntaService.actualizarDesdeDTO(1L, dto)).thenReturn(actualizada);

        ResponseEntity<?> response = preguntaController.actualizar(1L, dto);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(actualizada, response.getBody());
    }

    @Test
    void actualizar_illegalArgument_devuelve400() {
        Pregunta p = pregunta(1L, Pregunta.EstadoPregunta.borrador);
        PreguntaDTO dto = dto(1L);
        when(preguntaService.obtenerPorId(1L)).thenReturn(Optional.of(p));
        when(authService.canEditPregunta(Pregunta.EstadoPregunta.borrador)).thenReturn(true);
        when(preguntaService.actualizarDesdeDTO(1L, dto)).thenThrow(new IllegalArgumentException("inválido"));

        ResponseEntity<?> response = preguntaController.actualizar(1L, dto);

        assertEquals(400, response.getStatusCodeValue());
        assertEquals("inválido", response.getBody());
    }

    @Test
    void actualizar_optimisticLock_devuelve409() {
        Pregunta p = pregunta(1L, Pregunta.EstadoPregunta.borrador);
        PreguntaDTO dto = dto(1L);
        when(preguntaService.obtenerPorId(1L)).thenReturn(Optional.of(p));
        when(authService.canEditPregunta(Pregunta.EstadoPregunta.borrador)).thenReturn(true);
        when(preguntaService.actualizarDesdeDTO(1L, dto))
                .thenThrow(new ObjectOptimisticLockingFailureException("conflicto", new RuntimeException()));

        ResponseEntity<?> response = preguntaController.actualizar(1L, dto);

        assertEquals(409, response.getStatusCodeValue());
    }

    @Test
    void actualizar_excepcion_devuelve400() {
        Pregunta p = pregunta(1L, Pregunta.EstadoPregunta.borrador);
        PreguntaDTO dto = dto(1L);
        when(preguntaService.obtenerPorId(1L)).thenReturn(Optional.of(p));
        when(authService.canEditPregunta(Pregunta.EstadoPregunta.borrador)).thenReturn(true);
        when(preguntaService.actualizarDesdeDTO(1L, dto)).thenThrow(new RuntimeException("fail"));

        ResponseEntity<?> response = preguntaController.actualizar(1L, dto);

        assertEquals(400, response.getStatusCodeValue());
    }

    @Test
    void cambiarEstado_noEncontrada_devuelve404() {
        when(preguntaService.obtenerPorId(9L)).thenReturn(Optional.empty());

        ResponseEntity<?> response = preguntaController.cambiarEstado(9L, Pregunta.EstadoPregunta.verificada);

        assertEquals(404, response.getStatusCodeValue());
    }

    @Test
    void cambiarEstado_sinPermiso_devuelve403() {
        Pregunta p = pregunta(1L, Pregunta.EstadoPregunta.borrador);
        when(preguntaService.obtenerPorId(1L)).thenReturn(Optional.of(p));
        when(authService.canChangeEstadoPregunta(Pregunta.EstadoPregunta.borrador, Pregunta.EstadoPregunta.verificada))
                .thenReturn(false);
        when(authService.getCurrentUser()).thenReturn(Optional.of(usuario(Usuario.RolUsuario.ROLE_GUION)));

        ResponseEntity<?> response = preguntaController.cambiarEstado(1L, Pregunta.EstadoPregunta.verificada);

        assertEquals(403, response.getStatusCodeValue());
        assertTrue(response.getBody().toString().contains("verificada"));
    }

    @Test
    void cambiarEstado_ok_devuelve200() {
        Pregunta p = pregunta(1L, Pregunta.EstadoPregunta.borrador);
        Pregunta actualizada = pregunta(1L, Pregunta.EstadoPregunta.para_verificar);
        Usuario user = usuario(Usuario.RolUsuario.ROLE_GUION);
        when(preguntaService.obtenerPorId(1L)).thenReturn(Optional.of(p), Optional.of(actualizada));
        when(authService.canChangeEstadoPregunta(Pregunta.EstadoPregunta.borrador, Pregunta.EstadoPregunta.para_verificar))
                .thenReturn(true);
        when(authService.getCurrentUser()).thenReturn(Optional.of(user));
        when(preguntaService.cambiarEstadoAtomico(1L, Pregunta.EstadoPregunta.borrador,
                Pregunta.EstadoPregunta.para_verificar, user)).thenReturn(true);

        ResponseEntity<?> response = preguntaController.cambiarEstado(1L, Pregunta.EstadoPregunta.para_verificar);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(actualizada, response.getBody());
    }

    @Test
    void cambiarEstado_illegalState_devuelve409() {
        Pregunta p = pregunta(1L, Pregunta.EstadoPregunta.borrador);
        Usuario user = usuario(Usuario.RolUsuario.ROLE_GUION);
        when(preguntaService.obtenerPorId(1L)).thenReturn(Optional.of(p));
        when(authService.canChangeEstadoPregunta(Pregunta.EstadoPregunta.borrador, Pregunta.EstadoPregunta.para_verificar))
                .thenReturn(true);
        when(authService.getCurrentUser()).thenReturn(Optional.of(user));
        doThrow(new IllegalStateException("conflicto")).when(preguntaService)
                .cambiarEstadoAtomico(1L, Pregunta.EstadoPregunta.borrador, Pregunta.EstadoPregunta.para_verificar, user);

        ResponseEntity<?> response = preguntaController.cambiarEstado(1L, Pregunta.EstadoPregunta.para_verificar);

        assertEquals(409, response.getStatusCodeValue());
        assertTrue(response.getBody().toString().contains("Conflicto de concurrencia"));
    }

    @Test
    void cambiarEstado_optimisticLock_devuelve409() {
        Pregunta p = pregunta(1L, Pregunta.EstadoPregunta.borrador);
        Usuario user = usuario(Usuario.RolUsuario.ROLE_GUION);
        when(preguntaService.obtenerPorId(1L)).thenReturn(Optional.of(p));
        when(authService.canChangeEstadoPregunta(Pregunta.EstadoPregunta.borrador, Pregunta.EstadoPregunta.para_verificar))
                .thenReturn(true);
        when(authService.getCurrentUser()).thenReturn(Optional.of(user));
        doThrow(new ObjectOptimisticLockingFailureException("conflicto", new RuntimeException()))
                .when(preguntaService).cambiarEstadoAtomico(1L, Pregunta.EstadoPregunta.borrador,
                Pregunta.EstadoPregunta.para_verificar, user);

        ResponseEntity<?> response = preguntaController.cambiarEstado(1L, Pregunta.EstadoPregunta.para_verificar);

        assertEquals(409, response.getStatusCodeValue());
    }

    @Test
    void cambiarEstado_excepcion_devuelve400() {
        Pregunta p = pregunta(1L, Pregunta.EstadoPregunta.borrador);
        when(preguntaService.obtenerPorId(1L)).thenReturn(Optional.of(p));
        when(authService.canChangeEstadoPregunta(Pregunta.EstadoPregunta.borrador, Pregunta.EstadoPregunta.para_verificar))
                .thenReturn(true);
        when(authService.getCurrentUser()).thenReturn(Optional.of(usuario(Usuario.RolUsuario.ROLE_GUION)));
        doThrow(new RuntimeException("fail")).when(preguntaService)
                .cambiarEstadoAtomico(any(), any(), any(), any());

        ResponseEntity<?> response = preguntaController.cambiarEstado(1L, Pregunta.EstadoPregunta.para_verificar);

        assertEquals(400, response.getStatusCodeValue());
    }

    @Test
    void eliminarPregunta_sinPermiso_devuelve403() {
        when(authService.canDelete()).thenReturn(false);

        ResponseEntity<?> response = preguntaController.eliminarPregunta(1L);

        assertEquals(403, response.getStatusCodeValue());
    }

    @Test
    void eliminarPregunta_noEncontrada_devuelve404() {
        when(authService.canDelete()).thenReturn(true);
        when(preguntaService.obtenerPorId(9L)).thenReturn(Optional.empty());

        ResponseEntity<?> response = preguntaController.eliminarPregunta(9L);

        assertEquals(404, response.getStatusCodeValue());
    }

    @Test
    void eliminarPregunta_ok_devuelve200() {
        when(authService.canDelete()).thenReturn(true);
        when(preguntaService.obtenerPorId(1L)).thenReturn(Optional.of(pregunta(1L, Pregunta.EstadoPregunta.borrador)));
        doNothing().when(preguntaService).eliminarPorId(1L);

        ResponseEntity<?> response = preguntaController.eliminarPregunta(1L);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals("Pregunta eliminada exitosamente", response.getBody());
    }

    @Test
    void eliminarPregunta_illegalArgument_devuelve400() {
        when(authService.canDelete()).thenReturn(true);
        when(preguntaService.obtenerPorId(1L)).thenReturn(Optional.of(pregunta(1L, Pregunta.EstadoPregunta.borrador)));
        doThrow(new IllegalArgumentException("no")).when(preguntaService).eliminarPorId(1L);

        ResponseEntity<?> response = preguntaController.eliminarPregunta(1L);

        assertEquals(400, response.getStatusCodeValue());
        assertEquals("no", response.getBody());
    }

    @Test
    void eliminarPregunta_dataIntegrity_devuelve400() {
        when(authService.canDelete()).thenReturn(true);
        when(preguntaService.obtenerPorId(1L)).thenReturn(Optional.of(pregunta(1L, Pregunta.EstadoPregunta.borrador)));
        doThrow(new DataIntegrityViolationException("fk")).when(preguntaService).eliminarPorId(1L);

        ResponseEntity<?> response = preguntaController.eliminarPregunta(1L);

        assertEquals(400, response.getStatusCodeValue());
        assertTrue(response.getBody().toString().contains("cuestionarios"));
    }

    @Test
    void eliminarPregunta_accessDenied_devuelve403() {
        when(authService.canDelete()).thenReturn(true);
        when(preguntaService.obtenerPorId(1L)).thenReturn(Optional.of(pregunta(1L, Pregunta.EstadoPregunta.borrador)));
        doThrow(new AccessDeniedException("denied")).when(preguntaService).eliminarPorId(1L);

        ResponseEntity<?> response = preguntaController.eliminarPregunta(1L);

        assertEquals(403, response.getStatusCodeValue());
    }

    @Test
    void eliminarPregunta_excepcion_devuelve400() {
        when(authService.canDelete()).thenReturn(true);
        when(preguntaService.obtenerPorId(1L)).thenReturn(Optional.of(pregunta(1L, Pregunta.EstadoPregunta.borrador)));
        doThrow(new RuntimeException("fail")).when(preguntaService).eliminarPorId(1L);

        ResponseEntity<?> response = preguntaController.eliminarPregunta(1L);

        assertEquals(400, response.getStatusCodeValue());
    }

    @Test
    void obtenerPorNivel_ok_devuelve200() {
        when(preguntaService.obtenerPorNivel(Pregunta.NivelPregunta._1LS)).thenReturn(Collections.emptyList());

        ResponseEntity<List<Pregunta>> response = preguntaController.obtenerPorNivel(Pregunta.NivelPregunta._1LS);

        assertEquals(200, response.getStatusCodeValue());
    }

    @Test
    void obtenerPorNivel_excepcion_devuelve500() {
        when(preguntaService.obtenerPorNivel(Pregunta.NivelPregunta._1LS)).thenThrow(new RuntimeException("fail"));

        ResponseEntity<List<Pregunta>> response = preguntaController.obtenerPorNivel(Pregunta.NivelPregunta._1LS);

        assertEquals(500, response.getStatusCodeValue());
    }

    @Test
    void obtenerPorEstado_ok_devuelve200() {
        when(preguntaService.obtenerPorEstado(Pregunta.EstadoPregunta.borrador)).thenReturn(Collections.emptyList());

        ResponseEntity<List<Pregunta>> response = preguntaController.obtenerPorEstado(Pregunta.EstadoPregunta.borrador);

        assertEquals(200, response.getStatusCodeValue());
    }

    @Test
    void obtenerPorEstado_excepcion_devuelve500() {
        when(preguntaService.obtenerPorEstado(Pregunta.EstadoPregunta.borrador)).thenThrow(new RuntimeException("fail"));

        ResponseEntity<List<Pregunta>> response = preguntaController.obtenerPorEstado(Pregunta.EstadoPregunta.borrador);

        assertEquals(500, response.getStatusCodeValue());
    }

    @Test
    void obtenerDisponibles_ok_devuelve200() {
        when(preguntaService.obtenerDisponibles()).thenReturn(Collections.emptyList());

        ResponseEntity<List<Pregunta>> response = preguntaController.obtenerDisponibles();

        assertEquals(200, response.getStatusCodeValue());
    }

    @Test
    void obtenerDisponibles_excepcion_devuelve500() {
        when(preguntaService.obtenerDisponibles()).thenThrow(new RuntimeException("fail"));

        ResponseEntity<List<Pregunta>> response = preguntaController.obtenerDisponibles();

        assertEquals(500, response.getStatusCodeValue());
    }

    @Test
    void verificarPregunta_noAutenticado_devuelve401() {
        when(authService.getCurrentUser()).thenReturn(Optional.empty());

        ResponseEntity<?> response = preguntaController.verificarPregunta(1L, true, "ok");

        assertEquals(401, response.getStatusCodeValue());
    }

    @Test
    void verificarPregunta_sinPermiso_devuelve403() {
        when(authService.getCurrentUser()).thenReturn(Optional.of(usuario(Usuario.RolUsuario.ROLE_GUION)));

        ResponseEntity<?> response = preguntaController.verificarPregunta(1L, true, "ok");

        assertEquals(403, response.getStatusCodeValue());
    }

    @Test
    void verificarPregunta_okAprobada_devuelve200() {
        Usuario user = usuario(Usuario.RolUsuario.ROLE_VERIFICACION);
        Pregunta p = pregunta(1L, Pregunta.EstadoPregunta.verificada);
        when(authService.getCurrentUser()).thenReturn(Optional.of(user));
        when(preguntaService.verificar(1L, Pregunta.EstadoPregunta.verificada, "ok", user)).thenReturn(p);

        ResponseEntity<?> response = preguntaController.verificarPregunta(1L, true, "ok");

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(p, response.getBody());
    }

    @Test
    void verificarPregunta_okRechazada_devuelve200() {
        Usuario user = usuario(Usuario.RolUsuario.ROLE_DIRECCION);
        Pregunta p = pregunta(1L, Pregunta.EstadoPregunta.rechazada);
        when(authService.getCurrentUser()).thenReturn(Optional.of(user));
        when(preguntaService.verificar(1L, Pregunta.EstadoPregunta.rechazada, "mal", user)).thenReturn(p);

        ResponseEntity<?> response = preguntaController.verificarPregunta(1L, false, "mal");

        assertEquals(200, response.getStatusCodeValue());
    }

    @Test
    void verificarPregunta_errorServicio_devuelve400() {
        Usuario user = usuario(Usuario.RolUsuario.ROLE_VERIFICACION);
        when(authService.getCurrentUser()).thenReturn(Optional.of(user));
        when(preguntaService.verificar(any(), any(), any(), any())).thenThrow(new RuntimeException("fail"));

        ResponseEntity<?> response = preguntaController.verificarPregunta(1L, true, null);

        assertEquals(400, response.getStatusCodeValue());
    }

    @Test
    void aprobarPregunta_noEncontrada_devuelve404() {
        when(preguntaService.obtenerPorId(9L)).thenReturn(Optional.empty());

        ResponseEntity<?> response = preguntaController.aprobarPregunta(9L);

        assertEquals(404, response.getStatusCodeValue());
    }

    @Test
    void aprobarPregunta_sinPermiso_devuelve403() {
        when(preguntaService.obtenerPorId(1L)).thenReturn(Optional.of(pregunta(1L, Pregunta.EstadoPregunta.verificada)));
        when(authService.canValidate()).thenReturn(false);

        ResponseEntity<?> response = preguntaController.aprobarPregunta(1L);

        assertEquals(403, response.getStatusCodeValue());
    }

    @Test
    void aprobarPregunta_estadoIncorrecto_devuelve400() {
        when(preguntaService.obtenerPorId(1L)).thenReturn(Optional.of(pregunta(1L, Pregunta.EstadoPregunta.borrador)));
        when(authService.canValidate()).thenReturn(true);

        ResponseEntity<?> response = preguntaController.aprobarPregunta(1L);

        assertEquals(400, response.getStatusCodeValue());
        assertTrue(response.getBody().toString().contains("borrador"));
    }

    @Test
    void aprobarPregunta_ok_devuelve200() {
        Pregunta p = pregunta(1L, Pregunta.EstadoPregunta.verificada);
        Pregunta aprobada = pregunta(1L, Pregunta.EstadoPregunta.aprobada);
        when(preguntaService.obtenerPorId(1L)).thenReturn(Optional.of(p), Optional.of(aprobada));
        when(authService.canValidate()).thenReturn(true);
        when(preguntaService.cambiarEstadoAtomico(1L, Pregunta.EstadoPregunta.verificada,
                Pregunta.EstadoPregunta.aprobada, null)).thenReturn(true);

        ResponseEntity<?> response = preguntaController.aprobarPregunta(1L);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(aprobada, response.getBody());
    }

    @Test
    void aprobarPregunta_desdeRevisar_devuelve200() {
        Pregunta p = pregunta(1L, Pregunta.EstadoPregunta.revisar);
        when(preguntaService.obtenerPorId(1L)).thenReturn(Optional.of(p));
        when(authService.canValidate()).thenReturn(true);
        when(preguntaService.cambiarEstadoAtomico(1L, Pregunta.EstadoPregunta.revisar,
                Pregunta.EstadoPregunta.aprobada, null)).thenReturn(true);

        ResponseEntity<?> response = preguntaController.aprobarPregunta(1L);

        assertEquals(200, response.getStatusCodeValue());
    }

    @Test
    void aprobarPregunta_illegalState_devuelve409() {
        Pregunta p = pregunta(1L, Pregunta.EstadoPregunta.verificada);
        when(preguntaService.obtenerPorId(1L)).thenReturn(Optional.of(p));
        when(authService.canValidate()).thenReturn(true);
        doThrow(new IllegalStateException("conflicto")).when(preguntaService)
                .cambiarEstadoAtomico(1L, Pregunta.EstadoPregunta.verificada, Pregunta.EstadoPregunta.aprobada, null);

        ResponseEntity<?> response = preguntaController.aprobarPregunta(1L);

        assertEquals(409, response.getStatusCodeValue());
    }

    @Test
    void aprobarPregunta_optimisticLock_devuelve409() {
        Pregunta p = pregunta(1L, Pregunta.EstadoPregunta.verificada);
        when(preguntaService.obtenerPorId(1L)).thenReturn(Optional.of(p));
        when(authService.canValidate()).thenReturn(true);
        doThrow(new ObjectOptimisticLockingFailureException("conflicto", new RuntimeException()))
                .when(preguntaService).cambiarEstadoAtomico(1L, Pregunta.EstadoPregunta.verificada,
                Pregunta.EstadoPregunta.aprobada, null);

        ResponseEntity<?> response = preguntaController.aprobarPregunta(1L);

        assertEquals(409, response.getStatusCodeValue());
    }

    @Test
    void rechazarPregunta_noEncontrada_devuelve404() {
        when(preguntaService.obtenerPorId(9L)).thenReturn(Optional.empty());

        ResponseEntity<?> response = preguntaController.rechazarPregunta(9L, "motivo");

        assertEquals(404, response.getStatusCodeValue());
    }

    @Test
    void rechazarPregunta_sinPermiso_devuelve403() {
        when(preguntaService.obtenerPorId(1L)).thenReturn(Optional.of(pregunta(1L, Pregunta.EstadoPregunta.verificada)));
        when(authService.canValidate()).thenReturn(false);

        ResponseEntity<?> response = preguntaController.rechazarPregunta(1L, "motivo");

        assertEquals(403, response.getStatusCodeValue());
    }

    @Test
    void rechazarPregunta_yaAprobada_devuelve400() {
        when(preguntaService.obtenerPorId(1L)).thenReturn(Optional.of(pregunta(1L, Pregunta.EstadoPregunta.aprobada)));
        when(authService.canValidate()).thenReturn(true);

        ResponseEntity<?> response = preguntaController.rechazarPregunta(1L, "motivo");

        assertEquals(400, response.getStatusCodeValue());
        assertTrue(response.getBody().toString().contains("ya aprobada"));
    }

    @Test
    void rechazarPregunta_motivoVacio_devuelve400() {
        when(preguntaService.obtenerPorId(1L)).thenReturn(Optional.of(pregunta(1L, Pregunta.EstadoPregunta.verificada)));
        when(authService.canValidate()).thenReturn(true);

        ResponseEntity<?> response = preguntaController.rechazarPregunta(1L, "  ");

        assertEquals(400, response.getStatusCodeValue());
        assertTrue(response.getBody().toString().contains("motivo"));
    }

    @Test
    void rechazarPregunta_ok_devuelve200() {
        Pregunta p = pregunta(1L, Pregunta.EstadoPregunta.verificada);
        Pregunta rechazada = pregunta(1L, Pregunta.EstadoPregunta.rechazada);
        when(preguntaService.obtenerPorId(1L)).thenReturn(Optional.of(p), Optional.of(rechazada));
        when(authService.canValidate()).thenReturn(true);
        when(preguntaService.rechazarAtomico(1L, Pregunta.EstadoPregunta.verificada, "mal")).thenReturn(true);

        ResponseEntity<?> response = preguntaController.rechazarPregunta(1L, "mal");

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(rechazada, response.getBody());
    }

    @Test
    void rechazarPregunta_illegalState_devuelve409() {
        Pregunta p = pregunta(1L, Pregunta.EstadoPregunta.verificada);
        when(preguntaService.obtenerPorId(1L)).thenReturn(Optional.of(p));
        when(authService.canValidate()).thenReturn(true);
        doThrow(new IllegalStateException("conflicto")).when(preguntaService)
                .rechazarAtomico(1L, Pregunta.EstadoPregunta.verificada, "mal");

        ResponseEntity<?> response = preguntaController.rechazarPregunta(1L, "mal");

        assertEquals(409, response.getStatusCodeValue());
    }

    @Test
    void rechazarPregunta_optimisticLock_devuelve409() {
        Pregunta p = pregunta(1L, Pregunta.EstadoPregunta.verificada);
        when(preguntaService.obtenerPorId(1L)).thenReturn(Optional.of(p));
        when(authService.canValidate()).thenReturn(true);
        doThrow(new ObjectOptimisticLockingFailureException("conflicto", new RuntimeException()))
                .when(preguntaService).rechazarAtomico(1L, Pregunta.EstadoPregunta.verificada, "mal");

        ResponseEntity<?> response = preguntaController.rechazarPregunta(1L, "mal");

        assertEquals(409, response.getStatusCodeValue());
    }

    @Test
    void marcarParaRevisar_noAutenticado_devuelve401() {
        when(authService.getCurrentUser()).thenReturn(Optional.empty());

        ResponseEntity<?> response = preguntaController.marcarParaRevisar(1L, "notas");

        assertEquals(401, response.getStatusCodeValue());
    }

    @Test
    void marcarParaRevisar_sinPermiso_devuelve403() {
        when(authService.getCurrentUser()).thenReturn(Optional.of(usuario(Usuario.RolUsuario.ROLE_GUION)));

        ResponseEntity<?> response = preguntaController.marcarParaRevisar(1L, "notas");

        assertEquals(403, response.getStatusCodeValue());
    }

    @Test
    void marcarParaRevisar_ok_devuelve200() {
        Usuario user = usuario(Usuario.RolUsuario.ROLE_ADMIN);
        Pregunta p = pregunta(1L, Pregunta.EstadoPregunta.revisar);
        when(authService.getCurrentUser()).thenReturn(Optional.of(user));
        when(preguntaService.marcarParaRevisar(1L, "notas", user)).thenReturn(p);

        ResponseEntity<?> response = preguntaController.marcarParaRevisar(1L, "notas");

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(p, response.getBody());
    }

    @Test
    void marcarParaRevisar_servicioNull_devuelve400() {
        Usuario user = usuario(Usuario.RolUsuario.ROLE_VERIFICACION);
        when(authService.getCurrentUser()).thenReturn(Optional.of(user));
        when(preguntaService.marcarParaRevisar(1L, null, user)).thenReturn(null);

        ResponseEntity<?> response = preguntaController.marcarParaRevisar(1L, null);

        assertEquals(400, response.getStatusCodeValue());
    }

    @Test
    void marcarParaRevisar_excepcion_devuelve400() {
        when(authService.getCurrentUser()).thenThrow(new RuntimeException("fail"));

        ResponseEntity<?> response = preguntaController.marcarParaRevisar(1L, "n");

        assertEquals(400, response.getStatusCodeValue());
    }

    @Test
    void marcarParaCorregir_noAutenticado_devuelve401() {
        when(authService.getCurrentUser()).thenReturn(Optional.empty());

        ResponseEntity<?> response = preguntaController.marcarParaCorregir(1L, "notas");

        assertEquals(401, response.getStatusCodeValue());
    }

    @Test
    void marcarParaCorregir_sinPermiso_devuelve403() {
        when(authService.getCurrentUser()).thenReturn(Optional.of(usuario(Usuario.RolUsuario.ROLE_VERIFICACION)));

        ResponseEntity<?> response = preguntaController.marcarParaCorregir(1L, "notas");

        assertEquals(403, response.getStatusCodeValue());
    }

    @Test
    void marcarParaCorregir_ok_devuelve200() {
        Usuario user = usuario(Usuario.RolUsuario.ROLE_DIRECCION);
        Pregunta p = pregunta(1L, Pregunta.EstadoPregunta.corregir);
        when(authService.getCurrentUser()).thenReturn(Optional.of(user));
        when(preguntaService.marcarParaCorregir(1L, "notas", user)).thenReturn(p);

        ResponseEntity<?> response = preguntaController.marcarParaCorregir(1L, "notas");

        assertEquals(200, response.getStatusCodeValue());
    }

    @Test
    void marcarParaCorregir_servicioNull_devuelve400() {
        Usuario user = usuario(Usuario.RolUsuario.ROLE_ADMIN);
        when(authService.getCurrentUser()).thenReturn(Optional.of(user));
        when(preguntaService.marcarParaCorregir(1L, null, user)).thenReturn(null);

        ResponseEntity<?> response = preguntaController.marcarParaCorregir(1L, null);

        assertEquals(400, response.getStatusCodeValue());
    }

    @Test
    void marcarParaCorregir_excepcion_devuelve400() {
        when(authService.getCurrentUser()).thenThrow(new RuntimeException("fail"));

        ResponseEntity<?> response = preguntaController.marcarParaCorregir(1L, "n");

        assertEquals(400, response.getStatusCodeValue());
    }

    @Test
    void validarPregunta_valida_devuelve200() {
        Pregunta p = pregunta(1L, Pregunta.EstadoPregunta.borrador);
        DataTransformationService.ValidationResult result = new DataTransformationService.ValidationResult();
        when(preguntaService.validarPregunta(p)).thenReturn(result);

        ResponseEntity<?> response = preguntaController.validarPregunta(p);

        assertEquals(200, response.getStatusCodeValue());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals(true, body.get("valid"));
    }

    @Test
    void validarPregunta_invalida_devuelve400() {
        Pregunta p = pregunta(1L, Pregunta.EstadoPregunta.borrador);
        DataTransformationService.ValidationResult result = new DataTransformationService.ValidationResult();
        result.addError("pregunta", "corta");
        when(preguntaService.validarPregunta(p)).thenReturn(result);

        ResponseEntity<?> response = preguntaController.validarPregunta(p);

        assertEquals(400, response.getStatusCodeValue());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals(false, body.get("valid"));
    }

    @Test
    void validarPregunta_excepcion_devuelve400() {
        Pregunta p = pregunta(1L, Pregunta.EstadoPregunta.borrador);
        when(preguntaService.validarPregunta(p)).thenThrow(new RuntimeException("fail"));

        ResponseEntity<?> response = preguntaController.validarPregunta(p);

        assertEquals(400, response.getStatusCodeValue());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals(false, body.get("valid"));
    }

    @Test
    void transformarTexto_ok_devuelve200() {
        when(dataTransformationService.normalizarPregunta("p")).thenReturn("P");
        when(dataTransformationService.normalizarRespuesta("r")).thenReturn("R");
        when(dataTransformationService.normalizarTematica("t")).thenReturn("T");
        Map<String, String> datos = new HashMap<>();
        datos.put("pregunta", "p");
        datos.put("respuesta", "r");
        datos.put("tematica", "t");

        ResponseEntity<?> response = preguntaController.transformarTexto(datos);

        assertEquals(200, response.getStatusCodeValue());
    }

    @Test
    void transformarTexto_parcial_devuelve200() {
        when(dataTransformationService.normalizarPregunta("p")).thenReturn("P");
        Map<String, String> datos = new HashMap<>();
        datos.put("pregunta", "p");

        ResponseEntity<?> response = preguntaController.transformarTexto(datos);

        assertEquals(200, response.getStatusCodeValue());
    }

    @Test
    void transformarTexto_excepcion_devuelve400() {
        when(dataTransformationService.normalizarPregunta("p")).thenThrow(new RuntimeException("fail"));
        Map<String, String> datos = new HashMap<>();
        datos.put("pregunta", "p");

        ResponseEntity<?> response = preguntaController.transformarTexto(datos);

        assertEquals(400, response.getStatusCodeValue());
    }

    @Test
    void buscarPreguntas_okConContenido_devuelve200() {
        Pregunta p = pregunta(1L, Pregunta.EstadoPregunta.borrador);
        Page<Pregunta> page = new PageImpl<>(Collections.singletonList(p));
        PreguntaDTO mapped = dto(1L);
        when(preguntaService.buscarPreguntasPaginadas(any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(page);
        when(preguntaService.mapPreguntaToDTO(p)).thenReturn(mapped);

        ResponseEntity<Page<PreguntaDTO>> response =
                preguntaController.buscarPreguntas(null, null, null, null, null, null, 0, 20);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(1, response.getBody().getContent().size());
    }

    @Test
    void buscarPreguntas_okVacio_devuelve200() {
        when(preguntaService.buscarPreguntasPaginadas(any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        ResponseEntity<Page<PreguntaDTO>> response =
                preguntaController.buscarPreguntas(null, null, null, null, null, null, 0, 20);

        assertEquals(200, response.getStatusCodeValue());
        assertTrue(response.getBody().getContent().isEmpty());
    }

    @Test
    void buscarPreguntas_excepcion_devuelve400() {
        when(preguntaService.buscarPreguntasPaginadas(any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenThrow(new RuntimeException("fail"));

        ResponseEntity<Page<PreguntaDTO>> response =
                preguntaController.buscarPreguntas(null, null, null, null, null, null, 0, 20);

        assertEquals(400, response.getStatusCodeValue());
    }

    @Test
    void filtrarPreguntasCompleto_ok_devuelve200() {
        Page<PreguntaDTO> page = new PageImpl<>(Collections.singletonList(dto(1L)));
        when(preguntaService.filtrarPreguntasCompletoPaginado(
                any(), any(), any(), any(), any(), any(), any(), any(), isNull(), any(Pageable.class)))
                .thenReturn(page);

        ResponseEntity<Page<PreguntaDTO>> response = preguntaController.filtrarPreguntasCompleto(
                null, null, "aprobada", null, null, "q", "r", null, null, 0, 25, "id", "desc");

        assertEquals(200, response.getStatusCodeValue());
    }

    @Test
    void filtrarPreguntasCompleto_conTextoAsc_devuelve200() {
        Page<PreguntaDTO> page = new PageImpl<>(Collections.emptyList());
        when(preguntaService.filtrarPreguntasCompletoPaginado(
                any(), any(), any(), any(), any(), isNull(), isNull(), any(), eq("hola"), any(Pageable.class)))
                .thenReturn(page);

        ResponseEntity<Page<PreguntaDTO>> response = preguntaController.filtrarPreguntasCompleto(
                null, null, null, null, null, "q", "r", "hola", null, 0, 25, "id", "asc");

        assertEquals(200, response.getStatusCodeValue());
    }

    @Test
    void filtrarPreguntasCompleto_excepcion_devuelve500() {
        when(preguntaService.filtrarPreguntasCompletoPaginado(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenThrow(new RuntimeException("fail"));

        ResponseEntity<Page<PreguntaDTO>> response = preguntaController.filtrarPreguntasCompleto(
                null, null, null, null, null, null, null, null, null, 0, 25, "id", "desc");

        assertEquals(500, response.getStatusCodeValue());
    }

    @Test
    void buscarApariciones_textoVacio_devuelve400() {
        ResponseEntity<?> response = preguntaController.buscarApariciones("  ");

        assertEquals(400, response.getStatusCodeValue());
    }

    @Test
    void buscarApariciones_ok_devuelve200() {
        when(preguntaService.buscarApariciones("madrid")).thenReturn(Collections.singletonList(dto(1L)));

        ResponseEntity<?> response = preguntaController.buscarApariciones("madrid");

        assertEquals(200, response.getStatusCodeValue());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals(1, body.get("totalApariciones"));
    }

    @Test
    void buscarApariciones_excepcion_devuelve500() {
        when(preguntaService.buscarApariciones("x")).thenThrow(new RuntimeException("fail"));

        ResponseEntity<?> response = preguntaController.buscarApariciones("x");

        assertEquals(500, response.getStatusCodeValue());
    }

    @Test
    void obtenerTematicasPreguntas_ok_devuelve200() {
        when(preguntaRepository.findDistinctTematicas()).thenReturn(Collections.singletonList("HISTORIA"));

        ResponseEntity<List<String>> response = preguntaController.obtenerTematicasPreguntas();

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void obtenerTematicasPreguntas_excepcion_devuelve500() {
        when(preguntaRepository.findDistinctTematicas()).thenThrow(new RuntimeException("fail"));

        ResponseEntity<List<String>> response = preguntaController.obtenerTematicasPreguntas();

        assertEquals(500, response.getStatusCodeValue());
    }
}
