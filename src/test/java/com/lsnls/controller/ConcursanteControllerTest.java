package com.lsnls.controller;

import com.lsnls.dto.ConcursanteDTO;
import com.lsnls.service.ConcursanteService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConcursanteControllerTest {

    @Mock
    private ConcursanteService concursanteService;

    @InjectMocks
    private ConcursanteController concursanteController;

    private ConcursanteDTO dto(Long id, String nombre) {
        ConcursanteDTO c = new ConcursanteDTO();
        c.setId(id);
        c.setNombre(nombre);
        return c;
    }

    @Test
    void findAll_okAsc_devuelve200() {
        Page<ConcursanteDTO> page = new PageImpl<>(Collections.singletonList(dto(1L, "Ana")));
        when(concursanteService.findAllPaginatedWithFilters(
                any(Pageable.class), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(page);

        ResponseEntity<?> response = concursanteController.findAll(
                0, 25, "numeroConcursante", "asc", null, null, null, null, null, null, null, null, null);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(page, response.getBody());
    }

    @Test
    void findAll_okDesc_devuelve200() {
        Page<ConcursanteDTO> page = new PageImpl<>(Collections.emptyList());
        when(concursanteService.findAllPaginatedWithFilters(
                any(Pageable.class), eq("activo"), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(page);

        ResponseEntity<?> response = concursanteController.findAll(
                0, 25, "nombre", "desc", "activo", null, null, null, null, null, null, null, null);

        assertEquals(200, response.getStatusCodeValue());
    }

    @Test
    void findAll_excepcion_devuelve500() {
        when(concursanteService.findAllPaginatedWithFilters(
                any(Pageable.class), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull()))
                .thenThrow(new RuntimeException("fail"));

        ResponseEntity<?> response = concursanteController.findAll(
                0, 25, "id", "asc", null, null, null, null, null, null, null, null, null);

        assertEquals(500, response.getStatusCodeValue());
    }

    @Test
    void findAllWithoutPagination_ok_devuelve200() {
        when(concursanteService.findAll()).thenReturn(Collections.singletonList(dto(1L, "Ana")));

        ResponseEntity<?> response = concursanteController.findAllWithoutPagination();

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(1, ((List<?>) response.getBody()).size());
    }

    @Test
    void findAllWithoutPagination_excepcion_devuelve500() {
        when(concursanteService.findAll()).thenThrow(new RuntimeException("fail"));

        ResponseEntity<?> response = concursanteController.findAllWithoutPagination();

        assertEquals(500, response.getStatusCodeValue());
    }

    @Test
    void findById_ok_devuelve200() {
        ConcursanteDTO c = dto(1L, "Ana");
        when(concursanteService.findById(1L)).thenReturn(c);

        ResponseEntity<?> response = concursanteController.findById(1L);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(c, response.getBody());
    }

    @Test
    void findById_noEncontrado_devuelve404() {
        when(concursanteService.findById(9L)).thenReturn(null);

        ResponseEntity<?> response = concursanteController.findById(9L);

        assertEquals(404, response.getStatusCodeValue());
    }

    @Test
    void findById_excepcion_devuelve400() {
        when(concursanteService.findById(1L)).thenThrow(new RuntimeException("fail"));

        ResponseEntity<?> response = concursanteController.findById(1L);

        assertEquals(400, response.getStatusCodeValue());
    }

    @Test
    void findByEstado_vacio_devuelve400() {
        ResponseEntity<?> response = concursanteController.findByEstado("  ");

        assertEquals(400, response.getStatusCodeValue());
        assertTrue(response.getBody().toString().contains("estado es requerido"));
    }

    @Test
    void findByEstado_ok_devuelve200() {
        when(concursanteService.findByEstado("activo")).thenReturn(Collections.emptyList());

        ResponseEntity<?> response = concursanteController.findByEstado("activo");

        assertEquals(200, response.getStatusCodeValue());
    }

    @Test
    void findByEstado_excepcion_devuelve500() {
        when(concursanteService.findByEstado("activo")).thenThrow(new RuntimeException("fail"));

        ResponseEntity<?> response = concursanteController.findByEstado("activo");

        assertEquals(500, response.getStatusCodeValue());
    }

    @Test
    void findByProgramaId_ok_devuelve200() {
        when(concursanteService.findByProgramaId(3L)).thenReturn(Collections.emptyList());

        ResponseEntity<?> response = concursanteController.findByProgramaId(3L);

        assertEquals(200, response.getStatusCodeValue());
    }

    @Test
    void findByProgramaId_excepcion_devuelve500() {
        when(concursanteService.findByProgramaId(3L)).thenThrow(new RuntimeException("fail"));

        ResponseEntity<?> response = concursanteController.findByProgramaId(3L);

        assertEquals(500, response.getStatusCodeValue());
    }

    @Test
    void findConcursantesDisponibles_ok_devuelve200() {
        Page<ConcursanteDTO> page = new PageImpl<>(Collections.emptyList());
        when(concursanteService.findConcursantesSinProgramaPaginated(any(Pageable.class), isNull())).thenReturn(page);

        ResponseEntity<?> response = concursanteController.findConcursantesDisponibles(0, 10, null);

        assertEquals(200, response.getStatusCodeValue());
    }

    @Test
    void findConcursantesDisponibles_excepcion_devuelve500() {
        when(concursanteService.findConcursantesSinProgramaPaginated(any(Pageable.class), isNull()))
                .thenThrow(new RuntimeException("fail"));

        ResponseEntity<?> response = concursanteController.findConcursantesDisponibles(0, 10, null);

        assertEquals(500, response.getStatusCodeValue());
    }

    @Test
    void create_nombreVacio_devuelve400() {
        ResponseEntity<?> response = concursanteController.create(dto(null, "  "));

        assertEquals(400, response.getStatusCodeValue());
        assertTrue(response.getBody().toString().contains("nombre"));
    }

    @Test
    void create_duracionInvalida_devuelve400() {
        ConcursanteDTO c = dto(null, "Ana");
        c.setDuracion("25");

        ResponseEntity<?> response = concursanteController.create(c);

        assertEquals(400, response.getStatusCodeValue());
        assertTrue(response.getBody().toString().contains("MM:SS"));
    }

    @Test
    void create_ok_devuelve200() {
        ConcursanteDTO c = dto(null, "Ana");
        c.setDuracion("25:30");
        ConcursanteDTO creado = dto(8L, "Ana");
        when(concursanteService.create(c)).thenReturn(creado);

        ResponseEntity<?> response = concursanteController.create(c);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(creado, response.getBody());
    }

    @Test
    void create_runtimeCuestionarioEstado_devuelve400Asignacion() {
        ConcursanteDTO c = dto(null, "Ana");
        when(concursanteService.create(c)).thenThrow(new RuntimeException("cuestionario en estado incorrecto"));

        ResponseEntity<?> response = concursanteController.create(c);

        assertEquals(400, response.getStatusCodeValue());
        assertTrue(response.getBody().toString().contains("Error de asignación"));
    }

    @Test
    void create_runtimeComboEstado_devuelve400Asignacion() {
        ConcursanteDTO c = dto(null, "Ana");
        when(concursanteService.create(c)).thenThrow(new RuntimeException("combo en estado incorrecto"));

        ResponseEntity<?> response = concursanteController.create(c);

        assertEquals(400, response.getStatusCodeValue());
        assertTrue(response.getBody().toString().contains("Error de asignación"));
    }

    @Test
    void create_runtimeGenerica_devuelve400() {
        ConcursanteDTO c = dto(null, "Ana");
        when(concursanteService.create(c)).thenThrow(new RuntimeException("otro"));

        ResponseEntity<?> response = concursanteController.create(c);

        assertEquals(400, response.getStatusCodeValue());
        assertTrue(response.getBody().toString().contains("Error al crear concursante"));
    }

    @Test
    void update_ok_devuelve200() {
        ConcursanteDTO c = dto(1L, "Ana");
        when(concursanteService.update(1L, c)).thenReturn(c);

        ResponseEntity<?> response = concursanteController.update(1L, c);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(c, response.getBody());
    }

    @Test
    void update_optimisticLock_devuelve409() {
        ConcursanteDTO c = dto(1L, "Ana");
        when(concursanteService.update(1L, c))
                .thenThrow(new ObjectOptimisticLockingFailureException("conflicto", new RuntimeException()));

        ResponseEntity<?> response = concursanteController.update(1L, c);

        assertEquals(409, response.getStatusCodeValue());
    }

    @Test
    void update_excepcion_devuelve400() {
        ConcursanteDTO c = dto(1L, "Ana");
        when(concursanteService.update(1L, c)).thenThrow(new RuntimeException("fail"));

        ResponseEntity<?> response = concursanteController.update(1L, c);

        assertEquals(400, response.getStatusCodeValue());
    }

    @Test
    void updateCampo_ok_devuelve200() {
        Map<String, Object> campo = Collections.singletonMap("nombre", "Ana");
        ConcursanteDTO updated = dto(1L, "Ana");
        when(concursanteService.updateCampo(1L, campo)).thenReturn(updated);

        ResponseEntity<?> response = concursanteController.updateCampo(1L, campo);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(updated, response.getBody());
    }

    @Test
    void updateCampo_optimisticLock_devuelve409() {
        Map<String, Object> campo = Collections.singletonMap("nombre", "Ana");
        when(concursanteService.updateCampo(1L, campo))
                .thenThrow(new ObjectOptimisticLockingFailureException("conflicto", new RuntimeException()));

        ResponseEntity<?> response = concursanteController.updateCampo(1L, campo);

        assertEquals(409, response.getStatusCodeValue());
    }

    @Test
    void updateCampo_excepcion_devuelve400() {
        Map<String, Object> campo = Collections.singletonMap("nombre", "Ana");
        when(concursanteService.updateCampo(1L, campo)).thenThrow(new RuntimeException("fail"));

        ResponseEntity<?> response = concursanteController.updateCampo(1L, campo);

        assertEquals(400, response.getStatusCodeValue());
    }

    @Test
    void asignarAPrograma_ok_devuelve200() {
        ConcursanteDTO c = dto(1L, "Ana");
        when(concursanteService.asignarAPrograma(1L, 2L, 3)).thenReturn(c);

        ResponseEntity<?> response = concursanteController.asignarAPrograma(1L, 2L, 3);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(c, response.getBody());
    }

    @Test
    void asignarAPrograma_noEncontrado_devuelve404() {
        when(concursanteService.asignarAPrograma(1L, 2L, null))
                .thenThrow(new RuntimeException("concursante no encontrado"));

        ResponseEntity<?> response = concursanteController.asignarAPrograma(1L, 2L, null);

        assertEquals(404, response.getStatusCodeValue());
    }

    @Test
    void asignarAPrograma_runtime_devuelve400() {
        when(concursanteService.asignarAPrograma(1L, 2L, null))
                .thenThrow(new RuntimeException("ocupado"));

        ResponseEntity<?> response = concursanteController.asignarAPrograma(1L, 2L, null);

        assertEquals(400, response.getStatusCodeValue());
        assertTrue(response.getBody().toString().contains("Error al asignar"));
    }

    @Test
    void desasignarDePrograma_ok_devuelve200() {
        ConcursanteDTO c = dto(1L, "Ana");
        when(concursanteService.desasignarDePrograma(1L)).thenReturn(c);

        ResponseEntity<?> response = concursanteController.desasignarDePrograma(1L);

        assertEquals(200, response.getStatusCodeValue());
    }

    @Test
    void desasignarDePrograma_noEncontrado_devuelve404() {
        when(concursanteService.desasignarDePrograma(1L)).thenThrow(new RuntimeException("no encontrado"));

        ResponseEntity<?> response = concursanteController.desasignarDePrograma(1L);

        assertEquals(404, response.getStatusCodeValue());
    }

    @Test
    void desasignarDePrograma_runtime_devuelve400() {
        when(concursanteService.desasignarDePrograma(1L)).thenThrow(new RuntimeException("otro"));

        ResponseEntity<?> response = concursanteController.desasignarDePrograma(1L);

        assertEquals(400, response.getStatusCodeValue());
    }

    @Test
    void asignarAJornada_ok_devuelve200() {
        ConcursanteDTO c = dto(1L, "Ana");
        when(concursanteService.asignarAJornada(1L, 4L)).thenReturn(c);

        ResponseEntity<?> response = concursanteController.asignarAJornada(1L, 4L);

        assertEquals(200, response.getStatusCodeValue());
    }

    @Test
    void asignarAJornada_noEncontrado_devuelve404() {
        when(concursanteService.asignarAJornada(1L, 4L)).thenThrow(new RuntimeException("jornada no encontrado"));

        ResponseEntity<?> response = concursanteController.asignarAJornada(1L, 4L);

        assertEquals(404, response.getStatusCodeValue());
    }

    @Test
    void asignarAJornada_runtime_devuelve400() {
        when(concursanteService.asignarAJornada(1L, 4L)).thenThrow(new RuntimeException("otro"));

        ResponseEntity<?> response = concursanteController.asignarAJornada(1L, 4L);

        assertEquals(400, response.getStatusCodeValue());
    }

    @Test
    void desasignarDeJornada_ok_devuelve200() {
        ConcursanteDTO c = dto(1L, "Ana");
        when(concursanteService.desasignarDeJornada(1L)).thenReturn(c);

        ResponseEntity<?> response = concursanteController.desasignarDeJornada(1L);

        assertEquals(200, response.getStatusCodeValue());
    }

    @Test
    void desasignarDeJornada_noEncontrado_devuelve404() {
        when(concursanteService.desasignarDeJornada(1L)).thenThrow(new RuntimeException("no encontrado"));

        ResponseEntity<?> response = concursanteController.desasignarDeJornada(1L);

        assertEquals(404, response.getStatusCodeValue());
    }

    @Test
    void desasignarDeJornada_runtime_devuelve400() {
        when(concursanteService.desasignarDeJornada(1L)).thenThrow(new RuntimeException("otro"));

        ResponseEntity<?> response = concursanteController.desasignarDeJornada(1L);

        assertEquals(400, response.getStatusCodeValue());
    }

    @Test
    void subirFoto_ok_devuelve200() throws Exception {
        MultipartFile foto = mock(MultipartFile.class);
        when(concursanteService.subirFoto(1L, foto)).thenReturn("/fotos/1.jpg");

        ResponseEntity<?> response = concursanteController.subirFoto(1L, foto);

        assertEquals(200, response.getStatusCodeValue());
        @SuppressWarnings("unchecked")
        Map<String, String> body = (Map<String, String>) response.getBody();
        assertEquals("/fotos/1.jpg", body.get("url"));
    }

    @Test
    void subirFoto_excepcion_devuelve400() throws Exception {
        MultipartFile foto = mock(MultipartFile.class);
        when(concursanteService.subirFoto(1L, foto)).thenThrow(new RuntimeException("io"));

        ResponseEntity<?> response = concursanteController.subirFoto(1L, foto);

        assertEquals(400, response.getStatusCodeValue());
        @SuppressWarnings("unchecked")
        Map<String, String> body = (Map<String, String>) response.getBody();
        assertTrue(body.get("error").contains("io"));
    }

    @Test
    void delete_noEncontrado_devuelve404() {
        when(concursanteService.findById(9L)).thenReturn(null);

        ResponseEntity<?> response = concursanteController.delete(9L);

        assertEquals(404, response.getStatusCodeValue());
    }

    @Test
    void delete_ok_devuelve200() {
        when(concursanteService.findById(1L)).thenReturn(dto(1L, "Ana"));
        doNothing().when(concursanteService).delete(1L);

        ResponseEntity<?> response = concursanteController.delete(1L);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals("Concursante eliminado exitosamente", response.getBody());
    }

    @Test
    void delete_illegalArgument_devuelve400() {
        when(concursanteService.findById(1L)).thenReturn(dto(1L, "Ana"));
        doThrow(new IllegalArgumentException("bloqueado")).when(concursanteService).delete(1L);

        ResponseEntity<?> response = concursanteController.delete(1L);

        assertEquals(400, response.getStatusCodeValue());
        assertEquals("bloqueado", response.getBody());
    }

    @Test
    void delete_dataIntegrity_devuelve400() {
        when(concursanteService.findById(1L)).thenReturn(dto(1L, "Ana"));
        doThrow(new DataIntegrityViolationException("fk")).when(concursanteService).delete(1L);

        ResponseEntity<?> response = concursanteController.delete(1L);

        assertEquals(400, response.getStatusCodeValue());
        assertTrue(response.getBody().toString().contains("datos asociados"));
    }

    @Test
    void delete_excepcion_devuelve400() {
        when(concursanteService.findById(1L)).thenReturn(dto(1L, "Ana"));
        doThrow(new RuntimeException("fail")).when(concursanteService).delete(1L);

        ResponseEntity<?> response = concursanteController.delete(1L);

        assertEquals(400, response.getStatusCodeValue());
    }
}
